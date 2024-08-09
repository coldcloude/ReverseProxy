package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.util.function.Supplier;

public class LineBasedChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final Supplier<ChannelHandler> handlerCreator;

    public LineBasedChannelInitializer(Supplier<ChannelHandler> handlerCreator) {
        this.handlerCreator = handlerCreator;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE)).addLast(handlerCreator.get());
    }
}
