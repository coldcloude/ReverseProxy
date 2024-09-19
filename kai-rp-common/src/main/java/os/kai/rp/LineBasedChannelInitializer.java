package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.util.Arrays;
import java.util.function.Function;

public class LineBasedChannelInitializer extends ChainedChannelInitializer {
    public LineBasedChannelInitializer(Function<SocketChannel,ChannelHandler> handlerCreator) {
        super(Arrays.asList(
                ch->new LineBasedFrameDecoder(Integer.MAX_VALUE),
                handlerCreator
        ));
    }
}
