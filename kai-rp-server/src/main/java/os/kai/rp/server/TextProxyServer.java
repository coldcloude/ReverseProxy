package os.kai.rp.server;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyServer;

import java.util.function.BiConsumer;

public class TextProxyServer extends NettyServer {
    public TextProxyServer(String host,int port,long timeout,BiConsumer<String,ChannelHandlerContext> onConnect,BiConsumer<String,ChannelHandlerContext> onDisconnect) {
        super(new LineBasedChannelInitializer(()->new TextProxyClientHandler(timeout,onConnect,onDisconnect)),host,port);
    }
    public TextProxyServer(String host,int port,long timeout) {
        this(host,port,timeout,null,null);
    }
}
