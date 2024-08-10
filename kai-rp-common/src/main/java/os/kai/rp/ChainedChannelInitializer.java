package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.util.List;
import java.util.function.Supplier;

public class ChainedChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final List<Supplier<ChannelHandler>> creators;

    public ChainedChannelInitializer(List<Supplier<ChannelHandler>> creators) {
        this.creators = creators;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        for(Supplier<ChannelHandler> creator : creators){
            pipeline = pipeline.addLast(creator.get());
        }
    }
}
