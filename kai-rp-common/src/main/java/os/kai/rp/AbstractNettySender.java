package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractNettySender implements Consumer<String> {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final AtomicReference<Thread> thread = new AtomicReference<>();

    private final CountDownLatch threadStarted = new CountDownLatch(1);

    private final LinkedBlockingQueue<String> queue;

    private final ChannelHandlerContext ctx;

    public AbstractNettySender(ChannelHandlerContext ctx, LinkedBlockingQueue<String> queue) {
        this.ctx = ctx;
        this.queue = queue;
    }

    public AbstractNettySender(ChannelHandlerContext ctx) {
        this(ctx,new LinkedBlockingQueue<>());
    }

    protected abstract void write(ChannelHandlerContext ctx, String data);

    @Override
    public void accept(String s) {
        queue.offer(s);
    }

    public void start() {
        EXECUTOR.execute(()->{
            thread.set(Thread.currentThread());
            threadStarted.countDown();
            while(running.get()){
                if(!Thread.interrupted()){
                    try{
                        String data = queue.take();
                        write(ctx,data);
                    }
                    catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    public void shutdown(){
        running.set(false);
        boolean started = false;
        while(!started){
            if(!Thread.interrupted()){
                try{
                    threadStarted.await();
                    started = true;
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
        Thread t = thread.get();
        if(t!=null){
            t.interrupt();
        }
    }
}
