package os.kai.rp.http.server;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import os.kai.rp.http.HttpInputContext;
import os.kai.rp.util.Base64;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class TestServer {
    static class TestHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            ServletInputStream ins = request.getInputStream();
            ServletOutputStream outs = response.getOutputStream();
            int status = 500;
            try{
                String method = baseRequest.getMethod();
                System.err.println(method);
                HttpURI uri = baseRequest.getHttpURI();
                System.err.println(uri.toString());
                System.err.println(uri.getHost());
                System.err.println(uri.getPort());
                System.err.println(uri.getPathQuery());
                System.err.println(uri.getPath());
                System.err.println(uri.getQuery());
                System.err.println(uri.getFragment());
                Enumeration<String> headers = request.getHeaderNames();
                while(headers.hasMoreElements()){
                    String name = headers.nextElement();
                    String value = request.getHeader(name);
                    System.err.println(name+": "+value);
                }
                String path = uri.getPath().substring(1);
                if("POST".equals(method)){
                    List<byte[]> rst = new LinkedList<>();
                    HttpInputContext inctx = new HttpInputContext(ins,12);
                    boolean finished = false;
                    while(!finished){
                        finished = inctx.read();
                        //print
                        String b64 = Base64.encode(inctx.buf(),inctx.len());
                        System.err.println(b64);
                        //save
                        byte[] r = new byte[inctx.len()];
                        System.arraycopy(inctx.buf(),0,r,0,inctx.len());
                        rst.add(r);
                    }
                    int size = 0;
                    for(byte[] r : rst){
                        size += r.length;
                    }
                    byte[] rr = new byte[size];
                    int offset = 0;
                    for(byte[] r : rst){
                        System.arraycopy(r,0,rr,offset,r.length);
                        offset += r.length;
                    }
                    String rstr = new String(rr);
                    if(rstr.startsWith("path=")&&!rstr.contains("&")){
                        path = rstr.substring(5);
                    }
                }
                File f = new File(path);
                if(f.exists()){
                    try(FileInputStream fins = new FileInputStream(path)){
                        byte[] buf = new byte[1024];
                        int a;
                        while((a=fins.available())>0){
                            int len = Math.min(buf.length,a);
                            len = fins.read(buf,0,len);
                            outs.write(buf,0,len);
                            outs.flush();
                        }
                    }
                    status = 200;
                }
                else {
                    status = 404;
                    response.setStatus(404);
                }
            }
            finally{
                response.setStatus(status);
                outs.close();
                ins.close();
            }
        }
    }

    public static void main(String[] args) {
        File pwd = new File(".");
        System.err.println(pwd.getAbsolutePath());
        for(String name : pwd.list()){
            System.err.println(name);
        }
        HttpProxyServer server = new HttpProxyServer("0.0.0.0",13355,new TestHandler());
        server.start();
    }
}
