package os.kai.rp.socks5;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.RawBase64NettyMultiplexSender;
import os.kai.rp.TextProxyHub;
import os.kai.rp.util.Base64;
import os.kai.rp.DoubleLockSingleton;
import os.kai.rp.util.JacksonUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Socks5Hub {
    private static final int CLINET_TO_SERVER = -1;
    private static final int SERVER_TO_CLIENT = 1;
    private static final AtomicInteger nop = new AtomicInteger(20);
    public static void setThreadNum(int n){
        nop.set(n);
    }
    private static final DoubleLockSingleton<Socks5Hub> hub = new DoubleLockSingleton<>(Socks5Hub::new);
    public static Socks5Hub get(){
        return hub.get();
    }
    private final RawBase64NettyMultiplexSender sender = new RawBase64NettyMultiplexSender(nop.get());
    private final Map<String,ChannelHandlerContext> ctxMap = new ConcurrentHashMap<>();
    private final Map<String,String> proxyMap = new ConcurrentHashMap<>();
    private Socks5Hub(){
        sender.start();
    }
    public void register(String ssid){
        sender.register(ssid);
    }
    public void connect(String ssid,ChannelHandlerContext ctx){
        sender.connect(ssid,ctx);
        ctxMap.put(ssid,ctx);
    }
    public void relay(Socks5RelayEntity entity) {
        sender.accept(entity.getSsid(),entity.getData64());
    }
    public void close(String ssid, boolean disconnect){
        sender.unregister(ssid);
        ChannelHandlerContext ctx = ctxMap.remove(ssid);
        if(disconnect&&ctx!=null){
            ctx.disconnect();
        }
    }
    public void registerProxy(String ip, String sid){
        proxyMap.put(ip,sid);
    }
    public void unregisterProxy(String ip){
        proxyMap.remove(ip);
    }
    public void process(String data){
        if(data.startsWith(Socks5Constant.PREFIX_RELAY)){
            try{
                String json = data.substring(Socks5Constant.PREFIX_RELAY_LEN);
                Socks5RelayEntity r = JacksonUtil.parse(json,Socks5RelayEntity.class);
                relay(r);
            }
            catch(JsonProcessingException e){
                log.warn("relay parse error",e);
            }
        }
        else if(data.startsWith(Socks5Constant.PREFIX_CLOSE)){
            String ssid = data.substring(Socks5Constant.PREFIX_CLOSE_LEN);
            close(ssid,true);
        }
    }
    public void sendRequest(String ssid, String addr, int port) throws JsonProcessingException {
        Socks5RequestEntity entity = new Socks5RequestEntity();
        entity.setSsid(ssid);
        entity.setAddr(addr);
        entity.setPort(port);
        String json = JacksonUtil.stringify(entity);
        String sid = proxyMap.getOrDefault(addr,Socks5Constant.SID);
        TextProxyHub.get().sendToClient(sid,Socks5Constant.PREFIX_REQ+json);
    }
    public void sendClose(String ssid, String addr, int port) throws JsonProcessingException {
        String sid = proxyMap.getOrDefault(addr,Socks5Constant.SID);
        TextProxyHub.get().sendToServer(sid,Socks5Constant.PREFIX_CLOSE+ssid);
    }
    private void readAndSendRelay(String ssid, String addr, int port, ByteBuf bb, byte[] buffer, int direction) throws JsonProcessingException {
        int readable;
        while((readable=bb.readableBytes())>0){
            int len = Math.min(readable,buffer.length);
            bb.readBytes(buffer,0,len);
            Socks5RelayEntity entity = new Socks5RelayEntity();
            entity.setSsid(ssid);
            entity.setData64(Base64.encode(buffer,len));
            String json = JacksonUtil.stringify(entity);
            String data = Socks5Constant.PREFIX_RELAY+json;
            String sid = proxyMap.getOrDefault(addr,Socks5Constant.SID);
            switch(direction){
                case CLINET_TO_SERVER:
                    TextProxyHub.get().sendToServer(sid,data);
                    break;
                case SERVER_TO_CLIENT:
                    TextProxyHub.get().sendToClient(sid,data);
                    break;
                default:
                    break;
            }
        }
    }
    public void readAndSendRelayToServer(String ssid, String addr, int port, ByteBuf bb, byte[] buffer) throws JsonProcessingException {
        readAndSendRelay(ssid,addr,port,bb,buffer,CLINET_TO_SERVER);
    }
    public void readAndSendRelayToClient(String ssid, String addr, int port, ByteBuf bb, byte[] buffer) throws JsonProcessingException {
        readAndSendRelay(ssid,addr,port,bb,buffer,SERVER_TO_CLIENT);
    }
}
