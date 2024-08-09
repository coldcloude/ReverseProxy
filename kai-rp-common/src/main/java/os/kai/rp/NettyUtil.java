package os.kai.rp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public class NettyUtil {

    public static String readLine(Object msg){
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        return new String(req,StandardCharsets.UTF_8);
    }

    public static void writeLine(ChannelHandlerContext ctx, String line){
        byte[] req = line.getBytes(StandardCharsets.UTF_8);
        ByteBuf msg = Unpooled.copiedBuffer(req);
        ctx.write(msg);
        ctx.flush();
    }
}
