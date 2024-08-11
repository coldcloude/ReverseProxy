package os.kai.rp.util;

import java.util.function.Supplier;

public class DoubleLockSingleton<T> {
    private final Supplier<T> creator;
    private volatile T base;
    public DoubleLockSingleton(Supplier<T> creator) {
        this.creator = creator;
    }
    public T get(){
        if(creator!=null){
            if(base==null){
                synchronized(this){
                    if(base==null){
                        base = creator.get();
                    }
                }
            }
        }
        return base;
    }
}
