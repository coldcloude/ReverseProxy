package os.kai.rp.client;

import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyClient;

public class TextProxyClient extends NettyClient {
    public TextProxyClient(String host,int port,String sessionId,long timeout) {
        super(new LineBasedChannelInitializer(()->new TextProxyServerHandler(host,port,sessionId,timeout)),host,port);
    }
}
