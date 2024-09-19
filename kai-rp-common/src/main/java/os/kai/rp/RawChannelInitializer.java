package os.kai.rp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;

import java.util.Collections;
import java.util.function.Function;

public class RawChannelInitializer extends ChainedChannelInitializer {
    public RawChannelInitializer(Function<SocketChannel,ChannelHandler> handlerCreator) {
        super(Collections.singletonList(handlerCreator));
    }
}
