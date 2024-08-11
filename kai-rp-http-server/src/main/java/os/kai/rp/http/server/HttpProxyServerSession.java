package os.kai.rp.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.http.HttpResponseEntity;
import os.kai.rp.util.JacksonUtil;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class HttpProxyServerSession {
    private final String sessionId;
    private final long timeout;
    private final Map<String,Timer> timerMap = new ConcurrentHashMap<>();
    private final Map<String,Consumer<HttpResponseEntity>> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String,Consumer<HttpPayloadEntity>> payloadHandlerMap = new ConcurrentHashMap<>();
    private final Map<String,Runnable> closeHandlerMap = new ConcurrentHashMap<>();
    private final Map<String,Runnable> timeoutHandlerMap = new ConcurrentHashMap<>();
    public HttpProxyServerSession(String sessionId,long timeout) {
        this.sessionId = sessionId;
        this.timeout = timeout;
        TextProxyHub.get().registerServerReceiver(sessionId,data->{
            if(data.startsWith(HttpConstant.PREFIX_RES)){
                String json = data.substring(HttpConstant.PREFIX_RES_LEN);
                try{
                    HttpResponseEntity entity = JacksonUtil.parse(json,HttpResponseEntity.class);
                    Consumer<HttpResponseEntity> op = responseHandlerMap.get(entity.getHsid());
                    if(op!=null){
                        op.accept(entity);
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
                    Consumer<HttpPayloadEntity> op = payloadHandlerMap.get(entity.getHsid());
                    if(op!=null){
                        op.accept(entity);
                    }
                }
                catch(JsonProcessingException e){
                    log.warn("payload parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_CLOSE)){
                String hsid = data.substring(HttpConstant.PREFIX_CLOSE_LEN);
                Runnable op = closeHandlerMap.get(hsid);
                if(op!=null){
                    op.run();
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
    public void sendClose(String hsid) throws JsonProcessingException {
        TextProxyHub.get().sendToClient(sessionId,HttpConstant.PREFIX_CLOSE+hsid);
    }
    private void addTimeoutTimer(String hsid){
        timerMap.computeIfAbsent(hsid,k->{
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerMap.remove(hsid);
                    responseHandlerMap.remove(hsid);
                    payloadHandlerMap.remove(hsid);
                    closeHandlerMap.remove(hsid);
                    Runnable timeoutHandler = timeoutHandlerMap.remove(hsid);
                    if(timeoutHandler!=null){
                        timeoutHandler.run();
                    }
                }
            },timeout);
            return timer;
        });
    }
    public void onResponse(String hsid, Consumer<HttpResponseEntity> op){
        responseHandlerMap.put(hsid,op);
        addTimeoutTimer(hsid);
    }
    public void onPayload(String hsid, Consumer<HttpPayloadEntity> op){
        payloadHandlerMap.put(hsid,op);
        addTimeoutTimer(hsid);
    }
    public void onClose(String hsid, Runnable op){
        closeHandlerMap.put(hsid,()->{
            Timer timer = timerMap.remove(hsid);
            if(timer!=null){
                timer.cancel();
            }
            responseHandlerMap.remove(hsid);
            payloadHandlerMap.remove(hsid);
            closeHandlerMap.remove(hsid);
            timeoutHandlerMap.remove(hsid);
            op.run();
        });
        addTimeoutTimer(hsid);
    }
    public void onTimeout(String hsid, Runnable op){
        timeoutHandlerMap.put(hsid,op);
        addTimeoutTimer(hsid);
    }
}
