package os.kai.rp.socks5.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import os.kai.rp.NettyClient;
import os.kai.rp.RawChannelInitializer;

public class Socks5Client {
    private final NettyClient client;
    public Socks5Client(String host,int port,String ssid,NioEventLoopGroup group) {
        RawChannelInitializer initializer = new RawChannelInitializer(ch->new Socks5ServerHandler(ssid,host,port));
        client = new NettyClient(initializer,host,port,group);
    }
    public ChannelFuture startAsync(){
        return client.startAsync();
    }
}
