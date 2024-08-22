package os.kai.rp.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HttpProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Set<String> SPECIAL_HEADER_SET = new HashSet<>(Arrays.asList("Content-Length","Content-Type"));
    private static final AtomicLong gsn = new AtomicLong(0);
    private final HttpProxyServerSession session;
    public HttpProxyHandler(long timeout){
        session = new HttpProxyServerSession(HttpConstant.SID,timeout);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            System.err.println("METHOD:" + request.method());
            System.err.println("URI:" + request.uri());
            HttpHeaders headers = request.headers();
            for(Map.Entry<String,String> kv : headers){
                System.err.println(kv.getKey()+": "+kv.getValue());
            }
        }
        if (msg instanceof HttpContent) {
            ByteBuf bb = ((HttpContent)msg).content();
            byte[] buf = NettyUtil.readBytes(bb);
            String b64 = Base64.encode(buf);
            System.err.println(b64);
        }
    }
}
