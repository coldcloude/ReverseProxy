package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.function.BiConsumer;

public abstract class AbstractNettyMultiplexSender<T> implements BiConsumer<String,T> {
    private final AsyncMultiplexProcessor<T> processor;
    public AbstractNettyMultiplexSender(int nop){
        processor = new AsyncMultiplexProcessor<>(nop);
    }
    protected abstract void write(ChannelHandlerContext ctx, T data);
    public void register(String id){
        processor.register(id);
    }
    public void connect(String id, ChannelHandlerContext ctx){
        processor.set(id,data->write(ctx,data));
    }
    public void unregister(String id){
        processor.unregister(id);
    }
    public void shutdown(){
        processor.shutdown();
    }
    @Override
    public void accept(String id, T data) {
        processor.accept(id,data);
    }
    public void start(){
        processor.start();
    }
}
