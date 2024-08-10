package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.util.Arrays;
import java.util.function.Supplier;

public class LineBasedChannelInitializer extends ChainedChannelInitializer {
    public LineBasedChannelInitializer(Supplier<ChannelHandler> handlerCreator) {
        super(Arrays.asList(
                ()->new LineBasedFrameDecoder(Integer.MAX_VALUE),
                handlerCreator
        ));
    }
}
