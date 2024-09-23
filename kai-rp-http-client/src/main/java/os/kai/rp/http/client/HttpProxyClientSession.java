package os.kai.rp.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.AsyncMultiplexProcessor;
import os.kai.rp.TextProxyHub;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.http.HttpResponseEntity;
import os.kai.rp.util.*;

import java.util.function.Consumer;

@Slf4j
public class HttpProxyClientSession {
    @AllArgsConstructor
    private static class HttpData {
        private final HttpRequestEntity request;
        private final HttpPayloadEntity payload;
        private final boolean close;
    }
    private final String sessionId;
    private final AsyncMultiplexProcessor<HttpData> processor;
    private final NioEventLoopGroup group = new NioEventLoopGroup();
    public HttpProxyClientSession(String schema, String host, int port, String sessionId, int nop) {
        this.sessionId = sessionId;
        this.processor = new AsyncMultiplexProcessor<>(nop);
        TextProxyHub.get().registerClientReceiver(sessionId,data->{
            if(data.startsWith(HttpConstant.PREFIX_REQ)){
                String json = data.substring(HttpConstant.PREFIX_REQ_LEN);
                try{
                    HttpRequestEntity entity = JacksonUtil.parse(json,HttpRequestEntity.class);
                    String hsid = entity.getHsid();
                    if(processor.register(hsid)){
                        new HttpClient(schema,host,port,hsid,group,this).startAsync();
                    }
                    processor.accept(hsid,new HttpData(entity,null,false));
                }
                catch(JsonProcessingException e){
                    log.warn("response parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_PAYLOAD)){
                String json = data.substring(HttpConstant.PREFIX_PAYLOAD_LEN);
                try{
                    HttpPayloadEntity entity = JacksonUtil.parse(json,HttpPayloadEntity.class);
                    processor.accept(entity.getHsid(),new HttpData(null,entity,false));
                }
                catch(JsonProcessingException e){
                    log.warn("payload parse error: json="+json,e);
                }
            }
            else if(data.startsWith(HttpConstant.PREFIX_CLOSE)){
                String hsid = data.substring(HttpConstant.PREFIX_CLOSE_LEN);
                processor.accept(hsid,new HttpData(null,null,true));
            }
        });
    }
    public void start(){
        processor.start();
    }
    public void shutdown(){
        processor.shutdown();
    }
    public void sendResponse(HttpResponseEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_RES+json);
    }
    public void sendPayload(HttpPayloadEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_PAYLOAD+json);
    }
    public void sendClose(String hsid) {
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_CLOSE+hsid);
    }
    public void register(String hsid, Consumer<HttpRequestEntity> requestHandler, Consumer<HttpPayloadEntity> payloadHandler, Runnable closeHandler){
        processor.set(hsid,data->{
            if(data.request!=null){
                requestHandler.accept(data.request);
            }
            if(data.payload!=null){
                payloadHandler.accept(data.payload);
            }
            if(data.close){
                closeHandler.run();
            }
        });
    }
    public void unregister(String hsid){
        processor.unregister(hsid);
    }
}
