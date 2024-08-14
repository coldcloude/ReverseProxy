package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

import java.util.concurrent.LinkedBlockingQueue;

public class RawBase64NettySender extends AbstractNettySender<String> {

    public RawBase64NettySender(ChannelHandlerContext ctx) {
        super(ctx);
    }

    public RawBase64NettySender(ChannelHandlerContext ctx, LinkedBlockingQueue<String> queue) {
        super(ctx,queue);
    }

    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        byte[] bytes = Base64.decode(data);
        NettyUtil.writeRaw(ctx,bytes);
    }
}
