package os.kai.rp.server;

import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyServer;

public class TextProxyServer extends NettyServer {
    public TextProxyServer(String host,int port,long timeout) {
        super(new LineBasedChannelInitializer(()->new TextProxyClientHandler(timeout)),host,port);
    }
}
