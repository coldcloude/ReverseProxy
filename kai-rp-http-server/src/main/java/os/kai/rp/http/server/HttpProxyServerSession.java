package os.kai.rp.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.http.*;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.util.OpUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class HttpProxyServerSession {
    private static class HttpContext {
        private final AtomicReference<Consumer<HttpResponseEntity>> responseHandler = new AtomicReference<>();
        private final AtomicReference<Consumer<HttpPayloadEntity>> payloadHandler = new AtomicReference<>();
        private final AtomicReference<Runnable> closeHandler = new AtomicReference<>();
    }
    private static final HttpContext EMPTY_CONTEXT = new HttpContext();
    private final String sessionId;
    private final Map<String,HttpContext> contextMap = new ConcurrentHashMap<>();
    HttpProxyServerSession(String sessionId) {
        this.sessionId = sessionId;
        TextProxyHub.get().registerServerReceiver(sessionId,data->{
            if(data.startsWith(HttpConstant.PREFIX_RES)){
                String json = data.substring(HttpConstant.PREFIX_RES_LEN);
                try{
                    HttpResponseEntity entity = JacksonUtil.parse(json,HttpResponseEntity.class);
                    OpUtil.acceptIfValid(()->contextMap.getOrDefault(entity.getHsid(),EMPTY_CONTEXT).responseHandler,entity);
                }
                catch(JsonProcessingException e){
                    log.warn("response parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_PAYLOAD)){
                String json = data.substring(HttpConstant.PREFIX_PAYLOAD_LEN);
                try{
                    HttpPayloadEntity entity = JacksonUtil.parse(json,HttpPayloadEntity.class);
                    OpUtil.acceptIfValid(()->contextMap.getOrDefault(entity.getHsid(),EMPTY_CONTEXT).payloadHandler,entity);
                }
                catch(JsonProcessingException e){
                    log.warn("payload parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_CLOSE)){
                String hsid = data.substring(HttpConstant.PREFIX_CLOSE_LEN);
                OpUtil.runIfValid(()->{
                    HttpContext context = contextMap.remove(hsid);
                    return context==null?null:context.closeHandler;
                });
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
    public void register(String hsid, Consumer<HttpResponseEntity> responseHandler, Consumer<HttpPayloadEntity> payloadHandler, Runnable closeHandler){
        HttpContext context = new HttpContext();
        context.responseHandler.set(responseHandler);
        context.payloadHandler.set(payloadHandler);
        context.closeHandler.set(closeHandler);
        contextMap.put(hsid,context);
    }
}
