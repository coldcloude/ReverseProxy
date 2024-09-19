package os.kai.rp.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OpUtil {
    private OpUtil(){

    }
    public static void runIfValid(AtomicReference<Runnable> opRef){
        Runnable op = opRef.get();
        if(op!=null){
            op.run();
        }
    }
    public static <T> void acceptIfValid(AtomicReference<Consumer<T>> opRef, T value){
        Consumer<T> op = opRef.get();
        if(op!=null){
            op.accept(value);
        }
    }
    public static void runIfValid(Supplier<AtomicReference<Runnable>> opRefGetter){
        AtomicReference<Runnable> opRef = opRefGetter.get();
        if(opRef!=null){
            runIfValid(opRef);
        }
    }
    public static <T> void acceptIfValid(Supplier<AtomicReference<Consumer<T>>> opRefGetter, T value){
        AtomicReference<Consumer<T>> opRef = opRefGetter.get();
        if(opRef!=null){
            acceptIfValid(opRef,value);
        }
    }
}
