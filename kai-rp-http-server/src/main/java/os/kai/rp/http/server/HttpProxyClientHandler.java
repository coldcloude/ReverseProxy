package os.kai.rp.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class HttpProxyClientHandler extends ChannelInboundHandlerAdapter {
    private static final AtomicLong gsn = new AtomicLong(0);
    private final HttpProxyServerSession session;
    private final String httpSessionId;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    public HttpProxyClientHandler(HttpProxyServerSession session){
        this.session = session;
        httpSessionId = System.currentTimeMillis()+"-"+gsn.getAndIncrement();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.session.register(httpSessionId,res->{
            res.display(log);
            DefaultHttpResponse response = res.output();
            ctx.write(response);
            ctx.flush();
        },payload->{
            payload.display(log);
            DefaultHttpContent content = payload.output();
            ctx.write(content);
            ctx.flush();
        },()->{
            log.info(httpSessionId+" remote close");
            closed.set(true);
            ctx.disconnect();
        });
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpRequestEntity req = new HttpRequestEntity(httpSessionId,request);
            req.display(log);
            session.sendRequest(req);
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            HttpPayloadEntity payload = new HttpPayloadEntity(httpSessionId,content);
            payload.display(log);
            session.sendPayload(payload);
        }
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        session.unregister(httpSessionId);
        if(closed.compareAndSet(false,true)){
            log.info(httpSessionId+" close");
            session.sendClose(httpSessionId);
        }
    }
}
