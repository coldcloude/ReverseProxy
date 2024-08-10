package os.kai.rp;

import io.netty.channel.ChannelHandler;

import java.util.Collections;
import java.util.function.Supplier;

public class RawChannelInitializer extends ChainedChannelInitializer {
    public RawChannelInitializer(Supplier<ChannelHandler> handlerCreator) {
        super(Collections.singletonList(handlerCreator));
    }
}
