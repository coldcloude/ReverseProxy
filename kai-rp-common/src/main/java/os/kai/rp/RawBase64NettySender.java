package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

import java.util.concurrent.LinkedBlockingQueue;

public class RawBase64NettySender extends AbstractNettySender<String> {

    public RawBase64NettySender(ChannelHandlerContext ctx, int bufLen) {
        super(ctx,bufLen);
    }

    public RawBase64NettySender(ChannelHandlerContext ctx, int bufLen, LinkedBlockingQueue<String> queue) {
        super(ctx,bufLen,queue);
    }

    @Override
    protected void write(ChannelHandlerContext ctx,String data,byte[] buffer){
        Base64.decode(data,buffer,(bs,len)->NettyUtil.writeRaw(ctx,bs,0,len));
    }
}
