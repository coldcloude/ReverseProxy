package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public abstract class AbstractNettyMultiplexSender<T> extends Thread implements BiConsumer<String,T> {
    private class Context implements Runnable {
        private final AtomicReference<ChannelHandlerContext> ctx = new AtomicReference<>();
        private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
        @Override
        public synchronized void run() {
            ChannelHandlerContext c = ctx.get();
            if(c!=null){
                T data;
                while((data=queue.poll())!=null){
                    write(c,data);
                }
            }
        }
    }
    private final ExecutorService executor;
    private final LinkedBlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();
    private final Map<String,Context> contextMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    public AbstractNettyMultiplexSender(int nop){
        this.executor = Executors.newFixedThreadPool(nop);
    }
    protected abstract void write(ChannelHandlerContext ctx, T data);
    public void register(String id){
        contextMap.computeIfAbsent(id,k->new Context());
    }
    public void connect(String id,ChannelHandlerContext ctx){
        Context context = contextMap.get(id);
        if(context!=null){
            context.ctx.set(ctx);
            eventQueue.offer(id);
        }
    }
    public void unregister(String id){
        contextMap.remove(id);
    }
    public void shutdown(){
        running.set(false);
        executor.shutdown();
        interrupt();
    }
    @Override
    public void accept(String id, T data) {
        Context context = contextMap.get(id);
        if(context!=null){
            context.queue.offer(data);
            eventQueue.offer(id);
        }
    }
    @Override
    public void run(){
        while(running.get()){
            if(!interrupted()){
                try{
                    Set<String> idSet = new HashSet<>();
                    String curr = eventQueue.take();
                    while(curr!=null){
                        idSet.add(curr);
                        curr = eventQueue.poll();
                    }
                    for(String id : idSet){
                        Context context = contextMap.get(id);
                        if(context!=null){
                            executor.execute(context);
                        }
                    }
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
