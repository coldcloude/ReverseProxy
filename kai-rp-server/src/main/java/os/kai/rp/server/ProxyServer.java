package os.kai.rp.server;

import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyServer;

public class ProxyServer extends NettyServer {
    public ProxyServer(String host,int port,long timeout) {
        super(new LineBasedChannelInitializer(()->new ProxyClientHandler(timeout)),host,port);
    }
}
