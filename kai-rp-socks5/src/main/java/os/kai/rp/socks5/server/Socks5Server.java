package os.kai.rp.socks5.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.NettyServer;
import os.kai.rp.RawChannelInitializer;

@Slf4j
public class Socks5Server {
    private final NettyServer server;
    public Socks5Server(String host, int port){
        RawChannelInitializer initializer = new RawChannelInitializer(ch->new Socks5ClientHandler());
        server = new NettyServer(initializer,host,port);
    }
    public void start(){
        server.start();
    }
}
