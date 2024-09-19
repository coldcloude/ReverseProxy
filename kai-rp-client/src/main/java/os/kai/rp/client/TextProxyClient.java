package os.kai.rp.client;

import os.kai.rp.LineBasedChannelInitializer;
import os.kai.rp.NettyClient;

public class TextProxyClient {
    private final NettyClient client;
    public TextProxyClient(String host,int port,String sessionId,long timeout) {
        LineBasedChannelInitializer initializer = new LineBasedChannelInitializer(ch->new TextProxyServerHandler(host,port,sessionId,timeout));
        client = new NettyClient(initializer,host,port);
    }
    public void startWithRetry(int maxRetry, long interval){
        client.startWithRetry(maxRetry,interval);
    }
}
