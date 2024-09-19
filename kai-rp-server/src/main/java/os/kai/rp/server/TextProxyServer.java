package os.kai.rp.server;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyServer;

import java.util.function.BiConsumer;

public class TextProxyServer {
    private final NettyServer server;
    public TextProxyServer(String host,int port,long timeout,BiConsumer<String,ChannelHandlerContext> onConnect,BiConsumer<String,ChannelHandlerContext> onDisconnect) {
        LineBasedChannelInitializer initializer = new LineBasedChannelInitializer(ch->new TextProxyClientHandler(timeout,onConnect,onDisconnect));
        server = new NettyServer(initializer,host,port);
    }
    public TextProxyServer(String host,int port,long timeout) {
        this(host,port,timeout,null,null);
    }
    public void start(){
        server.start();
    }
}
