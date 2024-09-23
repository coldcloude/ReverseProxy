package os.kai.rp.http.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpResponseEntity;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class HttpProxyServerHandler extends ChannelInboundHandlerAdapter {
    private final String host;
    private final int port;
    private final HttpProxyClientSession session;
    private final String httpSessionId;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    public HttpProxyServerHandler(String host,int port,HttpProxyClientSession session,String httpSessionId) {
        this.host = host;
        this.port = port;
        this.session = session;
        this.httpSessionId = httpSessionId;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        session.register(httpSessionId,req->{
            //reset host if exists
            req.getHeaders().computeIfPresent("Host",(k,v)->host+":"+port);
            req.display(log);
            DefaultHttpRequest request = req.output();
            ctx.write(request);
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
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            HttpResponseEntity res = new HttpResponseEntity(httpSessionId,response);
            res.display(log);
            session.sendResponse(res);
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
