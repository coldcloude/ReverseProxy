package os.kai.rp.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class NettyUtil {
    public static String getRemoteAddress(ChannelHandlerContext ctx){
        InetSocketAddress addr = (InetSocketAddress)ctx.channel().remoteAddress();
        return addr.getAddress().getHostAddress();
    }
    public static byte[] readBytes(Object msg){
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        buf.release();
        return req;
    }
    public static String readString(Object msg){
        byte[] req = readBytes(msg);
        return new String(req,StandardCharsets.UTF_8);
    }
    public static void writeRawNoCopy(ChannelHandlerContext ctx,byte[] raw){
        ByteBuf msg = Unpooled.wrappedBuffer(raw);
        ctx.write(msg);
        ctx.flush();
    }
    public static void writeRaw(ChannelHandlerContext ctx, byte[] raw, int offset, int len){
        ByteBuf msg = Unpooled.copiedBuffer(raw,offset,len);
        ctx.write(msg);
        ctx.flush();
    }
    public static void writeLine(ChannelHandlerContext ctx, String line){
        byte[] req = (line+"\r\n").getBytes(StandardCharsets.UTF_8);
        writeRawNoCopy(ctx,req);
    }
}
