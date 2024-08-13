package os.kai.rp.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public class NettyUtil {

    public static String readLine(Object msg){
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String r = new String(req,StandardCharsets.UTF_8);
        buf.release();
        return r;
    }

    public static void writeRaw(ChannelHandlerContext ctx, byte[] raw){
        ByteBuf msg = Unpooled.copiedBuffer(raw);
        ctx.write(msg);
        ctx.flush();
    }

    public static void writeLine(ChannelHandlerContext ctx, String line){
        byte[] req = (line+"\r\n").getBytes(StandardCharsets.UTF_8);
        writeRaw(ctx,req);
    }
}
