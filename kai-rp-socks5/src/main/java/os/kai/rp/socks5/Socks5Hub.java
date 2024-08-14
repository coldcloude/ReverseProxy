package os.kai.rp.socks5;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.RawBase64NettyMultiplexSender;
import os.kai.rp.util.DoubleLockSingleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Socks5Hub {

    private static final AtomicInteger nop = new AtomicInteger(20);

    private static final DoubleLockSingleton<Socks5Hub> hub = new DoubleLockSingleton<>(Socks5Hub::new);

    public static Socks5Hub get(){
        return hub.get();
    }

    public static void setThreadNum(int n){
        nop.set(n);
    }

    private final RawBase64NettyMultiplexSender sender = new RawBase64NettyMultiplexSender(nop.get(),Socks5Constant.BUF_LEN);
    private final Map<String,ChannelHandlerContext> ctxMap = new ConcurrentHashMap<>();

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
}
