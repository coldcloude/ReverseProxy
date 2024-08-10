package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

public class RawBase64NettySender extends AbstractNettySender {

    public RawBase64NettySender(ChannelHandlerContext ctx) {
        super(ctx);
    }

    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        byte[] bytes = Base64.decode(data);
        NettyUtil.writeRaw(ctx,bytes);
    }
}
