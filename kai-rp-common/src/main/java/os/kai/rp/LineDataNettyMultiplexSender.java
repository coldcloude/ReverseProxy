package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.NettyUtil;

public class LineDataNettyMultiplexSender extends AbstractNettyMultiplexSender<String> {
    public LineDataNettyMultiplexSender(int nop) {
        super(nop);
    }
    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        String str = TextProxyTag.DATA_START+data+TextProxyTag.DATA_END;
        NettyUtil.writeLine(ctx,str);
    }
}
