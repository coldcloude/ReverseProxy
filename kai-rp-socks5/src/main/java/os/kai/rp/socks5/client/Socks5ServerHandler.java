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

    private final byte[] buffer = new byte[Socks5Constant.BUF_LEN];

    public Socks5ServerHandler(String ssid){
        this.ssid = ssid;
    }

    private String formatPrefix(){
        return "id="+ssid+": ";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String id = ctx.channel().id().asLongText();
        Socks5Hub.get().registerCtx(id,ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        ByteBuf bb = (ByteBuf)msg;
        Socks5Util.readAndSendRelay(ssid,bb,buffer);
        bb.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info(formatPrefix()+"disconnect");
        Socks5Hub.get().unregisterCtx(ssid);
        TextProxyHub.get().sendToServer(Socks5Constant.SID,Socks5Constant.PREFIX_CLOSE+ssid);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
        log.warn(formatPrefix(),cause);
        ctx.disconnect();
    }
}
