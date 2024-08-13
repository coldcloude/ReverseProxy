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
    private void process(String data){
        if(data.startsWith(Socks5Constant.PREFIX_RELAY)){
            try{
                String json = data.substring(Socks5Constant.PREFIX_RELAY_LEN);
                Socks5RelayEntity r = JacksonUtil.parse(json,Socks5RelayEntity.class);
                Socks5Hub.get().relay(r);
            }
            catch(JsonProcessingException e){
                log.warn("relay parse error",e);
            }
        }
        else if(data.startsWith(Socks5Constant.PREFIX_CLOSE)){
            String ssid = data.substring(Socks5Constant.PREFIX_CLOSE_LEN);
            Socks5Hub.get().close(ssid,true);
        }
    }
    public Socks5Server(String host, int port){
        super(new RawChannelInitializer(Socks5ClientHandler::new),host,port);
        TextProxyHub.get().registerServerReceiver(Socks5Constant.SID,this::process);
    }
}
