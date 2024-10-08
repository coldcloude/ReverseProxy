package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.NettyUtil;

import java.util.concurrent.LinkedBlockingQueue;

public class LineDataNettySender extends AbstractNettySender<String> {

    public LineDataNettySender(ChannelHandlerContext ctx) {
        super(ctx);
    }

    public LineDataNettySender(ChannelHandlerContext ctx, LinkedBlockingQueue<String> queue) {
        super(ctx,queue);
    }

    @Override
    protected void write(ChannelHandlerContext ctx,String data) {
        String str = TextProxyTag.DATA_START+data+TextProxyTag.DATA_END;
        NettyUtil.writeLine(ctx,str);
    }
}
