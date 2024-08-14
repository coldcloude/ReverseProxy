package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

public class RawBase64NettyMultiplexSender extends AbstractNettyMultiplexSender<String> {
    public RawBase64NettyMultiplexSender(int nop) {
        super(nop);
    }
    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        byte[] bs = Base64.decode(data);
        NettyUtil.writeRawNoCopy(ctx,bs);
    }
}
