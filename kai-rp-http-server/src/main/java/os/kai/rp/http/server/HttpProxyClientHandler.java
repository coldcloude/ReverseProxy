package os.kai.rp.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HttpProxyClientHandler extends ChannelInboundHandlerAdapter {
    private static final AtomicLong gsn = new AtomicLong(0);
    private final HttpProxyServerSession session;
    private final String httpSessionId = System.currentTimeMillis()+"-"+gsn.get();
    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();
    public HttpProxyClientHandler(HttpProxyServerSession session){
        this.session = session;
        this.session.register(httpSessionId,res->{
            ChannelHandlerContext ctx = context.get();
            if(ctx!=null){
                HttpVersion version = HttpVersion.valueOf(res.getVersion());
                HttpResponseStatus status = HttpResponseStatus.valueOf(res.getStatus());
                DefaultHttpResponse response = new DefaultHttpResponse(version,status);
                for(Map.Entry<String,String> kv : res.getHeaders().entrySet()){
                    response.headers().set(kv.getKey(),kv.getValue());
                }
                ctx.write(response);
            }
        },payload->{
            ChannelHandlerContext ctx = context.get();
            if(ctx!=null){
                byte[] bs = Base64.decode(payload.getData64());
                ByteBuf bb = Unpooled.wrappedBuffer(bs);
                DefaultHttpContent content = new DefaultHttpContent(bb);
                ctx.write(content);
            }
        },()->{
            ChannelHandlerContext ctx = context.get();
            if(ctx!=null){
                ctx.disconnect();
            }
        });
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpRequestEntity req = new HttpRequestEntity();
            req.setHsid(httpSessionId);
            req.setVersion(request.protocolVersion().text());
            req.setMethod(request.method().name());
            req.setPath(request.uri());
            System.err.println("METHOD:" + request.method());
            System.err.println("URI:" + request.uri());
            HttpHeaders headers = request.headers();
            for(Map.Entry<String,String> kv : headers){
                String key = kv.getKey();
                String value = kv.getValue();
                req.getHeaders().put(key,value);
                System.err.println(key+": "+value);
            }
            session.sendRequest(req);
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf bb = content.content();
            byte[] buf = NettyUtil.readBytes(bb);
            String b64 = Base64.encode(buf);
            HttpPayloadEntity payload = new HttpPayloadEntity();
            payload.setHsid(httpSessionId);
            payload.setData64(b64);
            System.err.println(b64);
            if(content instanceof LastHttpContent){
                payload.setLast(1);
                System.err.println("last");
            }
            session.sendPayload(payload);
        }
        super.channelRead(ctx,msg);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        session.sendClose(httpSessionId);
        super.channelInactive(ctx);
    }
}
