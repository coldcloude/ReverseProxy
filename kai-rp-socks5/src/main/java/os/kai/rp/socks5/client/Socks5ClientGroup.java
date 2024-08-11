package os.kai.rp.socks5.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.util.DoubleLockSingleton;
import os.kai.rp.TextProxyHub;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.Socks5RelayEntity;
import os.kai.rp.socks5.Socks5RequestEntity;
import os.kai.rp.util.Base64;
import os.kai.rp.util.JacksonUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Socks5ClientGroup {
    private static final DoubleLockSingleton<Socks5ClientGroup> group = new DoubleLockSingleton<>(Socks5ClientGroup::new);
    public static Socks5ClientGroup get(){
        return group.get();
    }
    private final Map<String,Socks5Client> clientMap = new ConcurrentHashMap<>();
    public void start(){
        TextProxyHub.get().registerClientReceiver(Socks5Constant.SID,data->{
            if(data.startsWith(Socks5Constant.PREFIX_REQ)){
                String json = data.substring(Socks5Constant.PREFIX_REQ_LEN);
                try{
                    Socks5RequestEntity r = JacksonUtil.parse(json,Socks5RequestEntity.class);
                    String ssid = r.getSsid();
                    int atyp = r.getAddrType();
                    byte[] addr = Base64.decode(r.getAddr64());
                    int port = r.getPort();
                    try {
                        String host;
                        if(atyp==Socks5Constant.ATYP_IPV4){
                            host = Inet4Address.getByAddress(addr).getHostAddress();
                        }
                        else if(atyp==Socks5Constant.ATYP_IPV6) {
                            host = Inet6Address.getByAddress(addr).getHostAddress();
                        }
                        else if(atyp==Socks5Constant.ATYP_DOMAIN) {
                            host = new String(addr,StandardCharsets.UTF_8);
                        }
                        else {
                            throw new InvalidAddressException("Unknown address type: "+atyp);
                        }
                        String logPrefix = "socks request [ssid="+ssid+", host="+host+", port="+port+"] ";
                        Socks5Client client = new Socks5Client(host,port,ssid);
                        ChannelFuture future = client.startAsync();
                        log.info(logPrefix+"started");
                        clientMap.put(ssid,client);
                        future.channel().closeFuture().addListener(f->{
                            Socks5Client c = clientMap.remove(ssid);
                            if(c!=null){
                                log.info(logPrefix+"ended");
                            }
                        });
                    } catch (UnknownHostException|InvalidAddressException e) {
                        log.warn("ssid="+ssid+", json="+json+": ",e);
                        TextProxyHub.get().sendToServer(Socks5Constant.SID,Socks5Constant.PREFIX_CLOSE+ssid);
                    }
                }
                catch(JsonProcessingException e){
                    log.warn("req parse error: json="+json,e);
                }
            }
            if(data.startsWith(Socks5Constant.PREFIX_RELAY)){
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
                Socks5Hub.get().close(ssid);
            }
        });
    }
}
