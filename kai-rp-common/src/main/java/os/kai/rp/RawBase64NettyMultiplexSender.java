package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

public class RawBase64NettyMultiplexSender extends AbstractNettyMultiplexSender<String> {
    public RawBase64NettyMultiplexSender(int nop, int bufLen) {
        super(nop,bufLen);
    }
    @Override
    protected void write(ChannelHandlerContext ctx,String data,byte[] buffer) {
        Base64.decode(data,buffer,(bs,len)->NettyUtil.writeRaw(ctx,bs,0,len));
    }
}
