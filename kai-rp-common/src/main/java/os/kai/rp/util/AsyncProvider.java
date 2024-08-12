package os.kai.rp.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class AsyncProvider<P,C> implements Iterable<C> {
    private final Function<P,C> transformer;
    private final LinkedBlockingQueue<P> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Lock lock = new ReentrantLock();
    private final AtomicBoolean checked = new AtomicBoolean(false);
    private final AtomicReference<C> current = new AtomicReference<>(null);
    private final AtomicReference<Thread> thread = new AtomicReference<>(null);
    private final Iterator<C> iterator;
    private class CheckIterator implements Iterator<C> {
        private void check(){
            if(!checked.get()){
                lock.lock();
                try{
                    if(!checked.get()){
                        thread.set(Thread.currentThread());
                        P data = null;
                        //not finished, wait for new data
                        while(!finished.get()){
                            try{
                                data = queue.take();
                                break;
                            }
                            catch(InterruptedException e){
                                //Thread.currentThread().interrupt();
                            }
                        }
                        //stop wait, try to get data with no block
                        if(data==null){
                            data = queue.poll();
                        }
                        //process data
                        if(data!=null){
                            C c = transformer.apply(data);
                            current.set(c);
                        }
                        else {
                            current.set(null);
                        }
                        checked.set(true);
                        thread.set(null);
                    }
                }
                finally{
                    lock.unlock();
                }
            }
        }
        @Override
        public boolean hasNext() {
            check();
            return current.get()!=null;
        }
        @Override
        public C next() {
            check();
            C curr = current.get();
            checked.set(false);
            if(curr!=null){
                return curr;
            }
            else {
                throw new NoSuchElementException();
            }
        }
    }
    public AsyncProvider(Function<P,C> transformer){
        this.transformer = transformer;
        iterator = new CheckIterator();
    }
    @Override
    public Iterator<C> iterator() {
        return iterator;
    }
    public void provide(P p) {
        queue.offer(p);
    }
    public void finish() {
        finished.set(true);
        Thread t = thread.get();
        if(t!=null){
            t.interrupt();
        }
    }
}
