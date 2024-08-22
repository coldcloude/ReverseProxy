package os.kai.rp.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.http.*;
import os.kai.rp.util.JacksonUtil;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class HttpProxyServerSession {
    private static class HttpContext {
        private final ChannelHandlerContext ctx;
        private final AtomicReference<Consumer<HttpResponseEntity>> responseHandler = new AtomicReference<>();
        private final AtomicReference<Consumer<HttpPayloadEntity>> payloadHandler = new AtomicReference<>();
        private HttpContext(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
    }
    private final String sessionId;
    private final Map<String,HttpContext> contextMap = new ConcurrentHashMap<>();
    public HttpProxyServerSession(String sessionId,long timeout) {
        this.sessionId = sessionId;
        TextProxyHub.get().registerServerReceiver(sessionId,data->{
            if(data.startsWith(HttpConstant.PREFIX_RES)){
                String json = data.substring(HttpConstant.PREFIX_RES_LEN);
                try{
                    HttpResponseEntity entity = JacksonUtil.parse(json,HttpResponseEntity.class);
                    HttpContext context = contextMap.get(entity.getHsid());
                    if(context!=null){
                        Consumer<HttpResponseEntity> op = context.responseHandler.get();
                        if(op!=null){
                            op.accept(entity);
                        }
                    }
                }
                catch(JsonProcessingException e){
                    log.warn("response parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_PAYLOAD)){
                String json = data.substring(HttpConstant.PREFIX_PAYLOAD_LEN);
                try{
                    HttpPayloadEntity entity = JacksonUtil.parse(json,HttpPayloadEntity.class);
                    HttpContext context = contextMap.get(entity.getHsid());
                    if(context!=null){
                        Consumer<HttpPayloadEntity> op = context.payloadHandler.get();
                        if(op!=null){
                            op.accept(entity);
                        }
                    }
                }
                catch(JsonProcessingException e){
                    log.warn("payload parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_CLOSE)){
                String hsid = data.substring(HttpConstant.PREFIX_CLOSE_LEN);
                HttpContext context = contextMap.remove(hsid);
                if(context!=null){
                    context.ctx.disconnect();
                }
            }
        });
    }
    public void sendRequest(HttpRequestEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToClient(sessionId,HttpConstant.PREFIX_REQ+json);
    }
    public void sendPayload(HttpPayloadEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToClient(sessionId,HttpConstant.PREFIX_PAYLOAD+json);
    }
    public void sendClose(String hsid) {
        TextProxyHub.get().sendToClient(sessionId,HttpConstant.PREFIX_CLOSE+hsid);
    }
    public void onResponse(String hsid, Consumer<HttpResponseEntity> op){
        HttpContext context = contextMap.get(hsid);
        if(context!=null){
            context.responseHandler.set(op);
        }
    }
    public void onPayload(String hsid, Consumer<HttpPayloadEntity> op){
        HttpContext context = contextMap.get(hsid);
        if(context!=null){
            context.payloadHandler.set(op);
        }
    }
}
