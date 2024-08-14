package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class AbstractNettySender<T> extends Thread implements Consumer<T> {

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final LinkedBlockingQueue<T> queue;

    private final ChannelHandlerContext ctx;

    private final byte[] buffer;

    public AbstractNettySender(ChannelHandlerContext ctx, int bufferSize, LinkedBlockingQueue<T> queue) {
        this.ctx = ctx;
        this.queue = queue;
        buffer = bufferSize>0?new byte[bufferSize]:null;
    }

    public AbstractNettySender(ChannelHandlerContext ctx, int bufferSize) {
        this(ctx,bufferSize,new LinkedBlockingQueue<>());
    }

    protected abstract void write(ChannelHandlerContext ctx, T data, byte[] buffer);

    @Override
    public void accept(T v) {
        queue.offer(v);
    }

    @Override
    public void run() {
        while(running.get()){
            if(!Thread.interrupted()){
                try{
                    T data = queue.take();
                    write(ctx,data,buffer);
                }
                catch(InterruptedException e){
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
