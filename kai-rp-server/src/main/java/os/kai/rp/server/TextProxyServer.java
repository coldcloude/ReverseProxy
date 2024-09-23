package os.kai.rp.server;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.LineDataNettyMultiplexSender;
import os.kai.rp.NettyServer;

import java.util.function.BiConsumer;

public class TextProxyServer {
    private final LineDataNettyMultiplexSender sender;
    private final NettyServer server;
    public TextProxyServer(String host,int port,int nop,long timeout,BiConsumer<String,ChannelHandlerContext> onConnect,BiConsumer<String,ChannelHandlerContext> onDisconnect) {
        sender = new LineDataNettyMultiplexSender(nop);
        LineBasedChannelInitializer initializer = new LineBasedChannelInitializer(ch->new TextProxyClientHandler(sender,timeout,onConnect,onDisconnect));
        server = new NettyServer(initializer,host,port);
    }
    public TextProxyServer(String host,int port, int nop, long timeout) {
        this(host,port,nop,timeout,null,null);
    }
    public void start(){
        sender.start();
        server.start();
    }
}
