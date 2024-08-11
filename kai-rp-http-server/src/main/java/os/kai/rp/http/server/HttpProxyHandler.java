package os.kai.rp.http.server;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpInputContext;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class HttpProxyHandler extends AbstractHandler {
    private static final AtomicLong gsn = new AtomicLong(0);
    private final HttpProxyServerSession session;
    public HttpProxyHandler(long timeout){
        session = new HttpProxyServerSession(HttpConstant.SID,timeout);
    }
    @Override
    public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException {
        String hsid = System.currentTimeMillis()+"-"+gsn.getAndIncrement();
        ServletInputStream ins = request.getInputStream();
        ServletOutputStream outs = response.getOutputStream();
        AtomicBoolean running = new AtomicBoolean(true);
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Thread currentThread = Thread.currentThread();
        byte[] buffer = new byte[HttpConstant.BUF_LEN];
        //set response callbacks
        session.onResponse(hsid,res->{
            response.setStatus(res.getStatus());
            for(Map.Entry<String,String> header : res.getHeaders().entrySet()){
                String name = header.getKey();
                String value = header.getValue();
                response.setHeader(name,value);
            }
        });
        session.onPayload(hsid,payload->queue.offer(payload.getData64()));
        session.onClose(hsid,()->{
            running.set(false);
            currentThread.interrupt();
        });
        try{
            //request
            HttpRequestEntity req = new HttpRequestEntity();
            req.setHsid(hsid);
            req.setMethod(baseRequest.getMethod());
            HttpURI uri = baseRequest.getHttpURI();
            req.setPath(uri.getPathQuery());
            Enumeration<String> headers = request.getHeaderNames();
            while(headers.hasMoreElements()){
                String name = headers.nextElement();
                String value = request.getHeader(name);
                req.getHeaders().put(name,value);
            }
            session.sendRequest(req);
            //payload
            HttpInputContext inctx = new HttpInputContext(ins,buffer);
            boolean finished = false;
            while(!finished){
                finished = inctx.read();
                int len = inctx.len();
                if(len>0){
                    HttpPayloadEntity payload = new HttpPayloadEntity();
                    payload.setHsid(hsid);
                    payload.setData64(Base64.encode(inctx.buf(),len));
                    session.sendPayload(payload);
                }
            }
            session.sendClose(hsid);
            //response payload
            while(running.get()){
                Thread.interrupted();
                try{
                    String b64 = queue.take();
                    int len = Base64.decode(b64,buffer);
                    outs.write(buffer,0,len);
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally{
            outs.close();
            ins.close();
        }
    }
}
