package os.kai.rp.socks5.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.*;
import os.kai.rp.socks5.*;
import os.kai.rp.util.Base64;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.util.NettyUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Socks5ClientHandler extends ChannelInboundHandlerAdapter {

    private static final int S5_STATE_NEG = 1;
    private static final int S5_STATE_REQ = 2;
    private static final int S5_STATE_RELAY = 3;

    private static final byte[] NEG_NO_AUTH = new byte[]{0x05,0x00};
    private static final byte[] REQ_LOCAL = new byte[]{0x05,0x00,0x00,0x01, 0x00,0x00,0x00,0x00, 0x00,0x00};

    private final FieldsReader negReader = new FieldsReader();
    private final FieldsReader reqReader = new FieldsReader();

    private final AtomicReference<String> ssid = new AtomicReference<>();
    private final AtomicInteger dstAddrType = new AtomicInteger();
    private final AtomicReference<byte[]> dstAddr = new AtomicReference<>();
    private final AtomicInteger dstPort = new AtomicInteger();

    private final byte[] buffer = new byte[Socks5Constant.BUF_LEN];

    /*
     *  request:
     *  +----+----------+----------+
     *  |VER | NMETHODS | METHODS  |
     *  +----+----------+----------+
     *  | 1  |    1     | 1 to 255 |
     *  +----+----------+----------+
     *
     *   response:
     *   +----+--------+
     *   |VER | METHOD |
     *   +----+--------+
     *   | 1  |   1    |
     *   +----+--------+
     */

    /**
     *   socks5 client request
     *   +----+-----+-------+------+----------+----------+
     *   |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
     *   +----+-----+-------+------+----------+----------+
     *   | 1  |  1  | X'00' |  1   | Variable |    2     |
     *   +----+-----+-------+------+----------+----------+
     *
     *   socks5 server response
     *   +----+-----+-------+------+----------+----------+
     *   |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
     *   +----+-----+-------+------+----------+----------+
     *   | 1  |  1  | X'00' |  1   | Variable |    2     |
     *   +----+-----+-------+------+----------+----------+
     */

    public Socks5ClientHandler(){
        //neg ver
        negReader.addFixedLengthProcessor(1,(bs,len)->{
            if(bs[0]!=0x05){
                throw new InvalidFieldException("wrong socks ver: "+bs[0]);
            }
        });
        //neg auth
        negReader.addVariableLengthProcessor((bs,len)->{
            boolean found = false;
            for(int i=0; i<len; i++){
                if(bs[i]==0x00){
                    found = true;
                    break;
                }
            }
            if(!found){
                throw new InvalidFieldException("no method with no auth");
            }
        });
        //req ver
        reqReader.addFixedLengthProcessor(1,(bs,len)->{
            if(bs[0]!=0x05){
                throw new InvalidFieldException("wrong socks ver: "+bs[0]);
            }
        });
        //req cmd
        reqReader.addFixedLengthProcessor(1,(bs,len)->{
            if(bs[0]!=0x01){
                throw new InvalidFieldException("unsupported socks cmd: "+bs[0]);
            }
        });
        //req rsv
        reqReader.addFixedLengthProcessor(1,(bs,len)->{});
        //req atyp
        reqReader.addFixedLengthProcessor(1,(bs,len)->{
            int type = ((int)bs[0])&0xFF;
            switch(type){
                case Socks5Constant.ATYP_IPV4:
                    //req addr ipv4
                    reqReader.addFixedLengthProcessor(4,(nbs,nlen)->{
                        byte[] ipv4 = new byte[4];
                        System.arraycopy(nbs,0,ipv4,0,4);
                        dstAddr.set(ipv4);
                    });
                    break;
                case Socks5Constant.ATYP_DOMAIN:
                    //req addr domain
                    reqReader.addVariableLengthProcessor((nbs,nlen)->{
                        byte[] domain = new byte[nlen];
                        System.arraycopy(nbs,0,domain,0,nlen);
                        domain = new String(domain).getBytes(StandardCharsets.UTF_8);
                        dstAddr.set(domain);
                    });
                    break;
                case Socks5Constant.ATYP_IPV6:
                    //req addr ipv6
                    reqReader.addFixedLengthProcessor(16,(nbs,nlen)->{
                        byte[] ipv6 = new byte[16];
                        System.arraycopy(nbs,0,ipv6,0,16);
                        dstAddr.set(ipv6);
                    });
                    break;
                default:
                    throw new InvalidFieldException("unknown addr type: "+type);
            }
            //req atyp
            dstAddrType.set(type);
            //req port
            reqReader.addFixedLengthProcessor(2,(nbs,nlen)->{
                int port = 0;
                port |= (((int)bs[0])&0x00FF)<<8;
                port |= ((int)bs[1])&0x00FF;
                dstPort.set(port);
            });
        });
    }

    private final AtomicInteger state = new AtomicInteger();

    private String formatPrefix(){
        return "ssid="+ssid.get()+", state="+state.get()+": ";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String id = ctx.channel().id().asLongText();
        ssid.set(id);
        state.set(S5_STATE_NEG);
        Socks5Hub.get().registerCtx(id,ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        int s = state.get();
        if(s==S5_STATE_NEG){
            boolean finished = negReader.read((ByteBuf)msg);
            if(finished){
                NettyUtil.writeRaw(ctx,NEG_NO_AUTH);
                state.set(S5_STATE_REQ);
            }
        }
        else if(s==S5_STATE_REQ){
            boolean finished = reqReader.read((ByteBuf)msg);
            if(finished){
                NettyUtil.writeRaw(ctx,REQ_LOCAL);
                Socks5RequestEntity entity = new Socks5RequestEntity();
                entity.setSsid(ssid.get());
                entity.setAddrType(dstAddrType.get());
                entity.setAddr64(Base64.encode(dstAddr.get()));
                entity.setPort(dstPort.get());
                String json = JacksonUtil.stringify(entity);
                ProxyHub.get().sendToClient(Socks5Constant.SID,Socks5Constant.PREFIX_REQ+json);
                state.set(S5_STATE_RELAY);
            }
        }
        else if(s==S5_STATE_RELAY){
            Socks5Util.readAndSendRelay(ssid.get(),(ByteBuf)msg,buffer);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"disconnect");
        Socks5Hub.get().unregisterCtx(ssid.get());
        ProxyHub.get().sendToClient(Socks5Constant.SID,Socks5Constant.PREFIX_CLOSE+ssid.get());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
        log.warn(formatPrefix(),cause);
        ctx.disconnect();
    }
}
