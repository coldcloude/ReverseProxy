package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.List;
import java.util.function.Function;

public class ChainedChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final List<Function<SocketChannel,ChannelHandler>> creators;

    public ChainedChannelInitializer(List<Function<SocketChannel,ChannelHandler>> creators) {
        this.creators = creators;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        for(Function<SocketChannel,ChannelHandler> creator : creators){
            ChannelHandler handler = creator.apply(ch);
            if(handler!=null){
                pipeline = pipeline.addLast();
            }
        }
    }
}
