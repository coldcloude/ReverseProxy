package os.kai.rp.client;

import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyClient;

public class ProxyClient extends NettyClient {
    public ProxyClient(String host,int port,String sessionId,long timeout) {
        super(new LineBasedChannelInitializer(()->new ProxyServerHandler(sessionId,timeout)),host,port);
    }
}
