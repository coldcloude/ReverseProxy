package os.kai.rp.socks5.client;

import os.kai.rp.NettyClient;
import os.kai.rp.RawChannelInitializer;

public class Socks5Client extends NettyClient {
    public Socks5Client(String host,int port,String ssid) {
        super(new RawChannelInitializer(()->new Socks5ServerHandler(ssid)),host,port);
    }
}
