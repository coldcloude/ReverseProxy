package os.kai.rp.socks5;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import os.kai.rp.RawBase64NettySender;
import os.kai.rp.util.DoubleLockSingleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Socks5Hub {

    private static final DoubleLockSingleton<Socks5Hub> hub = new DoubleLockSingleton<>(Socks5Hub::new);

    public static Socks5Hub get(){
        return hub.get();
    }

    @AllArgsConstructor
    private static class CtxSender {
        private final ChannelHandlerContext ctx;
        private final RawBase64NettySender sender;
    }

    private final Map<String,CtxSender> ctxSenderMap = new ConcurrentHashMap<>();

    public void registerCtx(String ssid,ChannelHandlerContext ctx){
        RawBase64NettySender sender = new RawBase64NettySender(ctx);
        sender.start();
        ctxSenderMap.put(ssid,new CtxSender(ctx,sender));
    }

    public void unregisterCtx(String ssid){
        CtxSender cs = ctxSenderMap.remove(ssid);
        if(cs!=null){
            cs.sender.shutdown();
        }
    }

    public void relay(Socks5RelayEntity entity) {
        CtxSender cs = ctxSenderMap.get(entity.getSsid());
        if(cs!=null){
            cs.sender.accept(entity.getData64());
        }
    }

    public void close(String ssid) {
        CtxSender cs = ctxSenderMap.get(ssid);
        if(cs!=null){
            cs.ctx.disconnect();
        }
    }
}
