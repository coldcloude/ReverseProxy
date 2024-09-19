package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.function.Consumer;

public abstract class AbstractNettySender<T> implements Consumer<T> {
    private final AsyncProcessor<T> processor = new AsyncProcessor<>();
    protected abstract void write(ChannelHandlerContext ctx, T data);
    @Override
    public void accept(T v) {
        processor.accept(v);
    }
    public void start(){
        processor.start();
    }
    public void shutdown(){
        processor.shutdown();
    }
    public void set(ChannelHandlerContext ctx){
        processor.set(data->write(ctx,data));
    }
}
