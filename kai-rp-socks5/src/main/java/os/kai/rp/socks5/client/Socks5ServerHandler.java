package os.kai.rp.socks5.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.Socks5Util;

@Slf4j
public class Socks5ServerHandler extends ChannelInboundHandlerAdapter {

    private final String ssid;

    private final String host;

    private final int port;

    private final byte[] buffer = new byte[Socks5Constant.BUF_LEN];

    public Socks5ServerHandler(String ssid, String host, int port){
        this.ssid = ssid;
        this.host = host;
        this.port = port;
    }

    private String formatPrefix(){
        return "id="+ssid+", host="+host+", port="+port+": ";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"connected");
        Socks5Hub.get().connect(ssid,ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        ByteBuf bb = (ByteBuf)msg;
        Socks5Util.readAndSendRelay(ssid,bb,buffer,Socks5Constant.CLIENT_TO_SERVER);
        bb.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"disconnected");
        Socks5Hub.get().close(ssid,false);
        TextProxyHub.get().sendToServer(Socks5Constant.SID,Socks5Constant.PREFIX_CLOSE+ssid);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
        log.warn(formatPrefix()+"error",cause);
        ctx.disconnect();
    }
}
