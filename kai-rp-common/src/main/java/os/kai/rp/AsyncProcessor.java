package os.kai.rp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AsyncProcessor<T> extends Thread implements Consumer<T> {

    private static final int STOPPED = 0;
    private static final int RUNNING = 1;
    private static final int DRILL = 2;

    private final AtomicInteger running = new AtomicInteger(RUNNING);

    private final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();

    private final AtomicReference<Consumer<T>> opRef = new AtomicReference<>();

    @Override
    public void accept(T v) {
        queue.offer(v);
    }

    @Override
    public void run() {
        while(running.get()==RUNNING){
            if(!Thread.interrupted()){
                try{
                    T data = queue.take();
                    Consumer<T> op = opRef.get();
                    if(op!=null){
                        op.accept(data);
                    }
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
        if(running.compareAndSet(DRILL,STOPPED)){
            Consumer<T> op = opRef.get();
            if(op!=null){
                T data;
                while((data=queue.poll())!=null){
                    op.accept(data);
                }
            }
        }
    }

    public void shutdown(boolean drill){
        running.set(drill?DRILL:STOPPED);
        interrupt();
    }

    public void shutdown(){
        shutdown(false);
    }

    public void set(Consumer<T> op){
        opRef.set(op);
    }
}
