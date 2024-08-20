package os.kai.rp.socks5.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.NettyServer;
import os.kai.rp.TextProxyHub;
import os.kai.rp.RawChannelInitializer;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.Socks5RelayEntity;

@Slf4j
public class Socks5Server extends NettyServer {
    public Socks5Server(String host, int port){
        super(new RawChannelInitializer(Socks5ClientHandler::new),host,port);
    }
}
