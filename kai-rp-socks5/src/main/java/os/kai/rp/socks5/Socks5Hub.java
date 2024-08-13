package os.kai.rp.socks5;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import os.kai.rp.RawBase64NettySender;
import os.kai.rp.util.DoubleLockSingleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class Socks5Hub {

    private static final DoubleLockSingleton<Socks5Hub> hub = new DoubleLockSingleton<>(Socks5Hub::new);

    public static Socks5Hub get(){
        return hub.get();
    }

    private static class Session {
        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private final AtomicReference<ChannelHandlerContext> ctx = new AtomicReference<>(null);
        private final AtomicReference<RawBase64NettySender> sender = new AtomicReference<>(null);
    }

    private final Map<String,Session> sessionMap = new ConcurrentHashMap<>();

    public void register(String ssid){
        sessionMap.put(ssid,new Session());
    }

    public void connect(String ssid,ChannelHandlerContext ctx){
        Session session = sessionMap.get(ssid);
        if(session!=null){
            RawBase64NettySender sender = new RawBase64NettySender(ctx,session.queue);
            sender.start();
            session.ctx.set(ctx);
            session.sender.set(sender);
        }
    }

    public void relay(Socks5RelayEntity entity) {
        Session session = sessionMap.get(entity.getSsid());
        if(session!=null){
            session.queue.offer(entity.getData64());
        }
    }

    public void close(String ssid, boolean disconnect){
        Session session = sessionMap.remove(ssid);
        if(session!=null){
            RawBase64NettySender sender = session.sender.get();
            if(sender!=null){
                sender.shutdown();
            }
            if(disconnect){
                ChannelHandlerContext ctx = session.ctx.get();
                if(ctx!=null){
                    ctx.disconnect();
                }
            }
        }
    }
}
