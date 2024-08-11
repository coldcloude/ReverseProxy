package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.NettyUtil;

public class LineDataNettySender extends AbstractNettySender {

    public LineDataNettySender(ChannelHandlerContext ctx) {
        super(ctx);
    }

    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        String str = TextProxyTag.DATA_START+data+TextProxyTag.DATA_END;
        NettyUtil.writeLine(ctx,str);
    }
}
