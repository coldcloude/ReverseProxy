package os.kai.rp.http;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpInputContext {
    private final Lock lock = new ReentrantLock();
    private final Condition readable = lock.newCondition();
    private final ServletInputStream ins;
    private final byte[] buf;
    private int len = 0;
    private volatile Throwable error = null;
    public HttpInputContext(ServletInputStream ins, byte[] buf){
        this.ins = ins;
        this.buf = buf;
        ins.setReadListener(new ReadListener() {
            @Override
            public void onDataAvailable() throws IOException {
                lock.lock();
                try{
                    readable.notify();
                }
                finally{
                    lock.unlock();
                }
            }
            @Override
            public void onAllDataRead() throws IOException {

            }
            @Override
            public void onError(Throwable t) {
                lock.lock();
                try{
                    error = t;
                    readable.notify();
                }
                finally{
                    lock.unlock();
                }
            }
        });
    }
    public HttpInputContext(ServletInputStream ins, int size){
        this(ins,new byte[size]);
    }
    public boolean read() throws IOException {
        if(ins.isFinished()){
            len = 0;
            return true;
        }
        while(!ins.isReady()){
            Thread.interrupted();
            lock.lock();
            try{
                try{
                    readable.await();
                    if(error!=null){
                        throw error instanceof IOException?(IOException)error:new IOException(error);
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
            finally{
                lock.unlock();
            }
        }
        int rest = Math.min(buf.length,ins.available());
        len = ins.read(buf,0,rest);
        len = Math.max(len,0);
        return ins.isFinished();
    }
    public byte[] buf(){
        return buf;
    }
    public int len(){
        return len;
    }
}
