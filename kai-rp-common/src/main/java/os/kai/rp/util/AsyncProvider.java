package os.kai.rp.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class AsyncProvider<P,C> implements Iterable<C> {
    private final Function<P,C> transformer;
    private final ConcurrentLinkedQueue<P> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Lock lock = new ReentrantLock();
    private final Condition nonEmpty = lock.newCondition();
    private final AtomicBoolean checked = new AtomicBoolean(false);
    private final AtomicReference<C> current = new AtomicReference<>(null);
    private final Iterator<C> iterator;
    private class CheckIterator implements Iterator<C> {
        private void check(){
            if(checked.compareAndSet(false,true)){
                P data = null;
                //not finished, wait for new data
                while((data=queue.poll())==null&&running.get()){
                    if(!Thread.interrupted()){
                        try{
                            nonEmpty.await();
                        }
                        catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                        }
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
            }
        }
        @Override
        public boolean hasNext() {
            boolean r;
            lock.lock();
            try{
                check();
                r = current.get()!=null;
            }
            finally{
                lock.unlock();
            }
            return r;
        }
        @Override
        public C next() {
            C r;
            lock.lock();
            try{
                check();
                r = current.get();
                checked.set(false);
            }
            finally{
                lock.unlock();
            }
            if(r!=null){
                return r;
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
        lock.lock();
        try{
            queue.offer(p);
            nonEmpty.signalAll();
        }
        finally{
            lock.unlock();
        }
    }
    public void finish() {
        lock.lock();
        try{
            running.set(false);
            nonEmpty.signalAll();
        }
        finally{
            lock.unlock();
        }
    }
}
