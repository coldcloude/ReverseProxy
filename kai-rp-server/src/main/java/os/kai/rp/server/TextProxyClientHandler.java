package os.kai.rp.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.LineDataNettyMultiplexSender;
import os.kai.rp.util.NettyUtil;
import os.kai.rp.TextProxyHub;
import os.kai.rp.TextProxyTag;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

@Slf4j
public class TextProxyClientHandler extends ChannelInboundHandlerAdapter {

    private static final int INIT = 0;
    private static final int PENDING = 1;
    private static final int RUNNING = 2;
    private static final int STOPPED = -1;

    private final LineDataNettyMultiplexSender sender;

    private final long timeout;

    private final AtomicLong lastUpdateTime = new AtomicLong();

    private void tick(){
        lastUpdateTime.set(System.currentTimeMillis());
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicInteger state = new AtomicInteger();

    private final BiConsumer<String,ChannelHandlerContext> onConnect;

    private final BiConsumer<String,ChannelHandlerContext> onDisconnect;

    private volatile Timer timer;

    private volatile String sessionId;

    private class Ticker extends TimerTask{
        private final ChannelHandlerContext ctx;
        private Ticker(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            if(time-lastUpdateTime.get()>timeout){
                ctx.disconnect();
            }
        }
    }

    public TextProxyClientHandler(LineDataNettyMultiplexSender sender, long timeout, BiConsumer<String,ChannelHandlerContext> onConnect, BiConsumer<String,ChannelHandlerContext> onDisconnect) {
        this.sender = sender;
        this.timeout = timeout;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        state.set(INIT);
        timer = null;
        sessionId = null;
    }

    public TextProxyClientHandler(LineDataNettyMultiplexSender sender, long timeout) {
        this(sender,timeout,null,null);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        int s = state.get();
        if(s==INIT){
            lock.writeLock().lock();
            try{
                s = state.get();
                if(s==INIT){
                    state.set(PENDING);
                    timer = new Timer();
                    timer.schedule(new Ticker(ctx),timeout/3,timeout/3);
                }
            }
            finally{
                lock.writeLock().unlock();
            }
        }
        tick();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int s = state.get();
        if(s!=STOPPED){
            lock.writeLock().lock();
            try{
                s = state.get();
                if(s!=STOPPED){
                    //stop state
                    state.set(STOPPED);
                    //stop timer
                    if(timer!=null){
                        timer.cancel();
                        timer = null;
                    }
                    //stop session
                    if(sessionId!=null){
                        TextProxyHub.get().unregisterClientReceiver(sessionId);
                        sender.unregister(sessionId);
                        sessionId = null;
                    }
                    //custom disconnect op
                    if(onDisconnect!=null){
                        onDisconnect.accept(sessionId,ctx);
                    }
                }
            }
            finally{
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        String body = NettyUtil.readString(msg);
        if(body.equals(TextProxyTag.KEEP_SINGLE)){
            tick();
        }
        else {
            String sid = TextProxyTag.unpackInit(body);
            if(sid!=null){
                int s = state.get();
                if(s==PENDING){
                    lock.writeLock().lock();
                    try{
                        s = state.get();
                        if(s==PENDING){
                            state.set(RUNNING);
                            //start sender
                            sender.register(sid);
                            sender.connect(sid,ctx);
                            //register receiver
                            TextProxyHub.get().registerClientReceiver(sid,data->sender.accept(sid,data));
                            sessionId = sid;
                            //custom connect op
                            if(onConnect!=null){
                                onConnect.accept(sid,ctx);
                            }
                            tick();
                        }
                    }
                    finally{
                        lock.writeLock().unlock();
                    }
                }
            }
            else {
                String data = TextProxyTag.unpackData(body);
                if(data!=null){
                    lock.readLock().lock();
                    try{
                        int s = state.get();
                        if(s==RUNNING&&sessionId!=null){
                            TextProxyHub.get().sendToServer(sessionId,data);
                            tick();
                        }
                    }
                    finally{
                        lock.readLock().unlock();
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
        log.warn("sessionId="+sessionId+", channelId="+ctx.channel().id().asShortText(),cause);
    }
}
