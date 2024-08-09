package os.kai.rp.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import os.kai.rp.ProxyTag;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private static final int INIT = 0;
    private static final int PENDING = 1;
    private static final int RUNNING = 2;
    private static final int STOPPED = -1;

    private final long timeout;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicInteger state = new AtomicInteger();

    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();

    private final AtomicReference<Timer> timer = new AtomicReference<>();

    private final AtomicLong lastUpdateTime = new AtomicLong();

    private final AtomicReference<String> sessionId = new AtomicReference<>();

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private static String unpack(String line, String startTag, String endTag){
        String r = null;
        int ll = line.length();
        if(ll>=ProxyTag.TAG_START_LEN+ProxyTag.TAG_END_LEN&&line.startsWith(startTag)&&line.endsWith(endTag)){
            r = line.substring(ProxyTag.TAG_START_LEN,ll-ProxyTag.TAG_END_LEN);
        }
        return r;
    }

    private void tick(){
        lastUpdateTime.set(System.currentTimeMillis());
    }

    private class Ticker extends TimerTask{
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            if(time-lastUpdateTime.get()>timeout){
                stop();
                lock.readLock().lock();
                try{
                    ChannelHandlerContext ctx = context.get();
                    if(ctx!=null){
                        ctx.disconnect();
                    }
                }
                finally{
                    lock.readLock().unlock();
                }
            }
        }
    }

    private class Sender extends Thread {
        @Override
        public void run() {
            while(state.get()==RUNNING){
                Thread.interrupted();
                try{
                    String data = queue.take();
                    lock.readLock().lock();
                    try{
                        int s = state.get();
                        ChannelHandlerContext ctx = context.get();
                        if(s==RUNNING&&ctx!=null){
                            String str = ProxyTag.DATA_START+data+ProxyTag.DATA_END;
                            ByteBuf msg = Unpooled.copiedBuffer(str.getBytes(StandardCharsets.UTF_8));
                            ctx.write(msg);
                            ctx.flush();
                        }
                    }
                    finally{
                        lock.readLock().unlock();
                    }
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public ProxyClientHandler(long timeout) {
        this.timeout = timeout;
        state.set(INIT);
        context.set(null);
        sessionId.set(null);
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
                    context.set(ctx);
                    Timer t = new Timer();
                    timer.set(t);
                    t.schedule(new Ticker(),timeout/3);
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
        stop();
        ChannelHandlerContext c = context.get();
        if(c!=null){
            lock.writeLock().lock();
            try{
                c = context.get();
                if(c!=null){
                    context.set(null);
                }
            }
            finally{
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req,StandardCharsets.UTF_8);
        body = body.trim();
        if(body.equals(ProxyTag.KEEP_SINGLE)){
            tick();
        }
        else {
            String sid = unpack(body,ProxyTag.INIT_START,ProxyTag.INIT_END);
            if(sid!=null){
                int s = state.get();
                if(s==PENDING){
                    lock.writeLock().lock();
                    try{
                        s = state.get();
                        if(s==PENDING){
                            state.set(RUNNING);
                            ProxyHub.get().registerClientReceiver(sid,queue::offer);
                            tick();
                        }
                    }
                    finally{
                        lock.writeLock().unlock();
                    }
                }
            }
            else {
                String data = unpack(body,ProxyTag.DATA_START,ProxyTag.DATA_END);
                if(data!=null){
                    lock.readLock().lock();
                    try{
                        int s = state.get();
                        sid = sessionId.get();
                        if(s==RUNNING&&sid!=null){
                            ProxyHub.get().sendToServer(sid,data);
                        }
                    }
                    finally{
                        lock.readLock().unlock();
                    }
                }
            }
        }
    }

    public void stop(){
        int s = state.get();
        if(s!=STOPPED){
            lock.writeLock().lock();
            try{
                s = state.get();
                if(s!=STOPPED){
                    //stop state
                    state.set(STOPPED);
                    //stop timer
                    Timer t = timer.get();
                    if(t!=null){
                        t.cancel();
                        timer.set(null);
                    }
                    //stop session
                    String sid = sessionId.get();
                    if(sid!=null){
                        ProxyHub.get().removeClientReceiver(sid);
                    }
                }
            }
            finally{
                lock.writeLock().unlock();
            }
        }
    }
}
