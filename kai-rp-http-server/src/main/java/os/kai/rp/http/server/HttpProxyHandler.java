package os.kai.rp.http.server;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpPayloadEntity;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.util.AsyncProvider;
import os.kai.rp.util.Base64;
import os.kai.rp.util.IOUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HttpProxyHandler extends AbstractHandler {
    private static final Set<String> SPECIAL_HEADER_SET = new HashSet<>(Arrays.asList("Content-Length","Content-Type"));
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
        AsyncProvider<String,String> provider = new AsyncProvider<>(v->v);
        session.onPayload(hsid,payload->provider.provide(payload.getData64()));
        session.onClose(hsid,provider::finish);
        try{
            //request
            HttpRequestEntity req = new HttpRequestEntity();
            req.setHsid(hsid);
            req.setMethod(baseRequest.getMethod());
            HttpURI uri = baseRequest.getHttpURI();
            req.setPath(uri.getPathQuery());
            req.setType(baseRequest.getContentType());
            req.setLength(baseRequest.getContentLength());
            Enumeration<String> headers = request.getHeaderNames();
            while(headers.hasMoreElements()){
                String name = headers.nextElement();
                String value = request.getHeader(name);
                if(!SPECIAL_HEADER_SET.contains(name)){
                    req.getHeaders().put(name,value);
                }
            }
            session.sendRequest(req);
            //payload
            IOUtil.readAll(ins,(bs,l)->{
                HttpPayloadEntity payload = new HttpPayloadEntity();
                payload.setHsid(hsid);
                payload.setData64(Base64.encode(bs,l));
                session.sendPayload(payload);
            },buffer);
            session.sendClose(hsid);
            //response payload
            for(String b64 : provider){
                int len = Base64.decode(b64,buffer);
                outs.write(buffer,0,len);
                outs.flush();
            }
        }
        finally{
            outs.close();
            ins.close();
        }
    }
}
