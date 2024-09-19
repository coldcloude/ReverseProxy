package os.kai.rp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AsyncMultiplexProcessor<T> extends Thread implements BiConsumer<String,T> {
    private class Context implements Runnable {
        private final AtomicReference<Consumer<T>> opRef = new AtomicReference<>();
        private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
        @Override
        public synchronized void run() {
            Consumer<T> op = opRef.get();
            if(op!=null){
                T data;
                while((data=queue.poll())!=null){
                    op.accept(data);
                }
            }
        }
    }
    private final ExecutorService executor;
    private final LinkedBlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();
    private final Map<String,Context> contextMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    public AsyncMultiplexProcessor(int nop){
        this.executor = Executors.newFixedThreadPool(nop);
    }
    public boolean register(String id){
        AtomicBoolean absent = new AtomicBoolean(false);
        contextMap.computeIfAbsent(id,k->{
            absent.set(true);
            return new Context();
        });
        return absent.get();
    }
    public void set(String id, Consumer<T> op){
        Context context = contextMap.get(id);
        if(context!=null){
            context.opRef.set(op);
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
