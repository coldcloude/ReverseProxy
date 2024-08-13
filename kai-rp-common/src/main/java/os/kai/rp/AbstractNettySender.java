package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class AbstractNettySender extends Thread implements Consumer<String> {

    private final AtomicBoolean running = new AtomicBoolean(true);

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

    @Override
    public void run() {
        while(running.get()){
            if(!Thread.interrupted()){
                try{
                    String data = queue.take();
                    write(ctx,data);
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown(){
        running.set(false);
        interrupt();
    }
}
