package os.kai.rp.socks5.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.Socks5RelayEntity;
import os.kai.rp.socks5.Socks5RequestEntity;
import os.kai.rp.util.JacksonUtil;

@Slf4j
public class Socks5ClientGroup {
    private final String sessionId;
    private final NioEventLoopGroup group = new NioEventLoopGroup();
    public Socks5ClientGroup(String sessionId) {
        this.sessionId = sessionId;
    }
    public void start(){
        TextProxyHub.get().registerClientReceiver(sessionId,data->{
            if(data.startsWith(Socks5Constant.PREFIX_REQ)){
                String json = data.substring(Socks5Constant.PREFIX_REQ_LEN);
                try{
                    Socks5RequestEntity r = JacksonUtil.parse(json,Socks5RequestEntity.class);
                    String ssid = r.getSsid();
                    String host = r.getAddr();
                    int port = r.getPort();
                    String logPrefix = "ssid="+ssid+", host="+host+", port="+port+": ";
                    Socks5Hub.get().register(ssid);
                    Socks5Client client = new Socks5Client(host,port,ssid,group);
                    client.startAsync();
                    log.info(logPrefix+"started");
                }
                catch(JsonProcessingException e){
                    log.warn("req parse error: json="+json,e);
                }
            }
            else if(data.startsWith(Socks5Constant.PREFIX_RELAY)){
                String json = data.substring(Socks5Constant.PREFIX_RELAY_LEN);
                try{
                    Socks5RelayEntity r = JacksonUtil.parse(json,Socks5RelayEntity.class);
                    Socks5Hub.get().relay(r);
                }
                catch(JsonProcessingException e){
                    log.warn("relay parse error: json="+json,e);
                }
            }
            else if(data.startsWith(Socks5Constant.PREFIX_CLOSE)){
                String ssid = data.substring(Socks5Constant.PREFIX_CLOSE_LEN);
                Socks5Hub.get().close(ssid,true);
            }
        });
    }
}
