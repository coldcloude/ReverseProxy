package os.kai.rp.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import os.kai.rp.TextProxyHub;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.http.HttpResponseEntity;
import os.kai.rp.util.Base64;
import os.kai.rp.util.IOUtil;
import os.kai.rp.util.JacksonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class HttpProxyClientSession {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final String schema;
    private final String host;
    private final int port;
    private final String sessionId;
    private final long timeout;
    private final HttpClient client;
    private final ConcurrentLinkedQueue<byte[]> bufferPool = new ConcurrentLinkedQueue<>();
    private final Map<String,Timer> timerMap = new ConcurrentHashMap<>();
    private final Map<String,Consumer<HttpPayloadEntity>> payloadHandlerMap = new ConcurrentHashMap<>();
    private final Map<String,Runnable> closeHandlerMap = new ConcurrentHashMap<>();
    public HttpProxyClientSession(String schema,String host,int port,String sessionId,long timeout) {
        this.schema = schema;
        this.host = host;
        this.port = port;
        this.sessionId = sessionId;
        this.timeout = timeout;
        client = new HttpClient(new SslContextFactory.Client(true));
    }
    public void start() throws Exception {
        client.start();
        client.getContentDecoderFactories().clear();
        client.setUserAgentField(null);
        TextProxyHub.get().registerClientReceiver(sessionId,data->{
            if(data.startsWith(HttpConstant.PREFIX_REQ)){
                String json = data.substring(HttpConstant.PREFIX_REQ_LEN);
                try{
                    HttpRequestEntity req = JacksonUtil.parse(json,HttpRequestEntity.class);
                    processRequest(req);
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
    private void processRequest(HttpRequestEntity req){
        String hsid = req.getHsid();
        String logPrefix = "request hsid="+hsid+": ";
        byte[] buf = bufferPool.poll();
        byte[] buffer = buf==null?new byte[HttpConstant.BUF_LEN]:buf;
        try{
            //handle timeout
            addTimeoutTimer(hsid);
            //set request header
            Request request = client.newRequest(schema+"://"+host+":"+port+req.getPath());
            String method = req.getMethod();
            request.method(method);
            for(Map.Entry<String,String> header : req.getHeaders().entrySet()){
                String name = header.getKey();
                String value = header.getValue();
                if(name.equals(HttpHeader.HOST.asString())){
                    request.header(name,host+":"+port);
                }
                else {
                    request.header(name,value);
                }
            }
            //set content
            boolean withBody = "POST".equals(method);
            if(withBody){
                request.content(new HttpProxyBase64ContentProvider(req.getLength(),buffer),req.getType());
                HttpProxyBase64ContentProvider provider = (HttpProxyBase64ContentProvider)request.getContent();
                onPayload(hsid,payload->provider.provide(payload.getData64()));
                onClose(hsid,provider::finish);
            }
            //async send
            EXECUTOR.execute(()->{
                InputStreamResponseListener listener = new InputStreamResponseListener();
                request.send(listener);
                Response response = null;
                boolean error = false;
                while(!error){
                    if(!Thread.interrupted()){
                        try{
                            response = listener.get(timeout,TimeUnit.MILLISECONDS);
                            break;
                        }
                        catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                        }
                        catch(TimeoutException|ExecutionException e){
                            log.warn(logPrefix,e);
                            error = true;
                        }
                    }
                }
                try{
                    if(error){
                        HttpResponseEntity res = new HttpResponseEntity();
                        res.setHsid(hsid);
                        res.setStatus(500);
                        sendResponse(res);
                        sendClose(hsid);
                    }
                    else if(response!=null){
                        HttpResponseEntity res = new HttpResponseEntity();
                        res.setHsid(hsid);
                        res.setStatus(response.getStatus());
                        for(HttpField header : response.getHeaders()){
                            res.getHeaders().put(header.getName(),header.getValue());
                        }
                        sendResponse(res);
                        try(InputStream ins = listener.getInputStream()){
                            IOUtil.readAll(ins,(bs,l)->{
                                HttpPayloadEntity payload = new HttpPayloadEntity();
                                payload.setHsid(hsid);
                                payload.setData64(Base64.encode(bs,l));
                                sendPayload(payload);
                            },buffer);
                        }
                        catch(IOException e){
                            log.warn(logPrefix,e);
                        }
                        sendClose(hsid);
                    }
                }
                catch(JsonProcessingException e){
                    log.warn(logPrefix,e);
                }
            });
        }
        finally{
            bufferPool.offer(buffer);
        }
    }
    private void sendResponse(HttpResponseEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_RES+json);
    }
    private void sendPayload(HttpPayloadEntity entity) throws JsonProcessingException {
        String json = JacksonUtil.stringify(entity);
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_PAYLOAD+json);
    }
    private void sendClose(String hsid){
        TextProxyHub.get().sendToServer(sessionId,HttpConstant.PREFIX_CLOSE+hsid);
    }
    private void addTimeoutTimer(String hsid){
        timerMap.computeIfAbsent(hsid,k->{
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerMap.remove(hsid);
                    payloadHandlerMap.remove(hsid);
                    closeHandlerMap.remove(hsid);
                }
            },timeout);
            return timer;
        });
    }
    private void onPayload(String hsid, Consumer<HttpPayloadEntity> op){
        payloadHandlerMap.put(hsid,op);
    }
    private void onClose(String hsid, Runnable op){
        closeHandlerMap.put(hsid,()->{
            Timer timer = timerMap.remove(hsid);
            if(timer!=null){
                timer.cancel();
            }
            payloadHandlerMap.remove(hsid);
            closeHandlerMap.remove(hsid);
            op.run();
        });
    }
}
