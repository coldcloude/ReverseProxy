package os.kai.rp.socks5.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.*;
import os.kai.rp.socks5.*;
import os.kai.rp.socks5.common.FieldsReader;
import os.kai.rp.socks5.common.InvalidFieldException;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.util.NettyUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Socks5ClientHandler extends ChannelInboundHandlerAdapter {

    private static final AtomicLong gsn = new AtomicLong(0);

    private static final int S5_STATE_NEG = 1;
    private static final int S5_STATE_REQ = 2;
    private static final int S5_STATE_RELAY = 3;

    private static final byte[] NEG_NO_AUTH = new byte[]{0x05,0x00};
    private static final byte[] REQ_LOCAL = new byte[]{0x05,0x00,0x00,0x01, 0x00,0x00,0x00,0x00, 0x00,0x00};

    private final FieldsReader negReader = new FieldsReader();
    private final FieldsReader reqReader = new FieldsReader();

    private final AtomicReference<String> logPrefix;

    private final String ssid;

    private final AtomicReference<String> dstAddr = new AtomicReference<>();
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

    /*
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
                        try{
                            String addr = Inet4Address.getByAddress(ipv4).getHostAddress();
                            dstAddr.set(addr);
                        }catch(UnknownHostException e){
                            throw new InvalidFieldException(e);
                        }
                    });
                    break;
                case Socks5Constant.ATYP_IPV6:
                    //req addr ipv6
                    reqReader.addFixedLengthProcessor(16,(nbs,nlen)->{
                        byte[] ipv6 = new byte[16];
                        System.arraycopy(nbs,0,ipv6,0,16);
                        try{
                            String addr = Inet6Address.getByAddress(ipv6).getHostAddress();
                            dstAddr.set(addr);
                        }catch(UnknownHostException e){
                            throw new InvalidFieldException(e);
                        }
                    });
                    break;
                case Socks5Constant.ATYP_DOMAIN:
                    //req addr domain
                    reqReader.addVariableLengthProcessor((nbs,nlen)->{
                        byte[] domain = new byte[nlen];
                        System.arraycopy(nbs,0,domain,0,nlen);
                        dstAddr.set(new String(domain));
                    });
                    break;
                default:
                    throw new InvalidFieldException("unknown addr type: "+type);
            }
            //req port
            reqReader.addFixedLengthProcessor(2,(nbs,nlen)->{
                int port = 0;
                port |= (((int)bs[0])&0x00FF)<<8;
                port |= ((int)bs[1])&0x00FF;
                dstPort.set(port);
            });
        });
        ssid = System.currentTimeMillis()+"-"+gsn.getAndIncrement();
        logPrefix = new AtomicReference<>("ssid="+ssid);
    }

    private final AtomicInteger state = new AtomicInteger();

    private String formatPrefix(){
        return logPrefix.get()+", state="+state.get()+": ";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"connected");
        state.set(S5_STATE_NEG);
        Socks5Hub.get().register(ssid);
        Socks5Hub.get().connect(ssid,ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        ByteBuf bb = (ByteBuf)msg;
        int s = state.get();
        if(s==S5_STATE_NEG){
            boolean finished = negReader.read(bb);
            if(finished){
                NettyUtil.writeRawNoCopy(ctx,NEG_NO_AUTH);
                state.set(S5_STATE_REQ);
                log.info(formatPrefix()+"negotiation replied");
            }
        }
        else if(s==S5_STATE_REQ){
            boolean finished = reqReader.read(bb);
            if(finished){
                NettyUtil.writeRawNoCopy(ctx,REQ_LOCAL);
                Socks5RequestEntity entity = new Socks5RequestEntity();
                entity.setSsid(ssid);
                entity.setAddr(dstAddr.get());
                entity.setPort(dstPort.get());
                String json = JacksonUtil.stringify(entity);
                TextProxyHub.get().sendToClient(Socks5Constant.SID,Socks5Constant.PREFIX_REQ+json);
                state.set(S5_STATE_RELAY);
                logPrefix.set(logPrefix.get()+", addr="+dstAddr.get()+", port="+dstPort.get());
                log.info(formatPrefix()+"request replied and sent");
            }
        }
        else if(s==S5_STATE_RELAY){
            Socks5Util.readAndSendRelayToClient(ssid,bb,buffer);
        }
        bb.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"disconnected");
        Socks5Hub.get().close(ssid,false);
        TextProxyHub.get().sendToClient(Socks5Constant.SID,Socks5Constant.PREFIX_CLOSE+ssid);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
        log.warn(formatPrefix(),cause);
        ctx.disconnect();
    }
}
