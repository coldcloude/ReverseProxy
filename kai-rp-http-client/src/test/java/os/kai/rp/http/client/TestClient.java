package os.kai.rp.http.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.HttpRequestEntity;
import os.kai.rp.http.client.HttpProxyBase64ContentProvider;
import os.kai.rp.util.Base64;
import os.kai.rp.util.IOUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TestClient {
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 13355;
        byte[] pl1 = "path=".getBytes(StandardCharsets.UTF_8);
        byte[] pl2 = "ReverseProxy-general.jpg".getBytes(StandardCharsets.UTF_8);
        HttpRequestEntity req = new HttpRequestEntity();
        req.setMethod("POST");
        req.setPath("/");
        req.setType("application/x-www-form-urlencoded");
        req.setLength(pl1.length+pl2.length);
        long timeout = 30000L;

        byte[] buffer = new byte[HttpConstant.BUF_LEN];
        HttpClient client = new HttpClient();
        client.start();

        Request request = client.newRequest("http://"+host+":"+port+req.getPath());
        request.method(req.getMethod());
        request.content(new HttpProxyBase64ContentProvider(req.getLength()),req.getType());
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);
        HttpProxyBase64ContentProvider provider = (HttpProxyBase64ContentProvider)request.getContent();
        Thread.sleep(3000L);
        provider.provide(Base64.encode(pl1));
        Thread.sleep(3000L);
        provider.provide(Base64.encode(pl2));
        Thread.sleep(3000L);
        provider.finish();

        Response response = listener.get(timeout,TimeUnit.MILLISECONDS);
        System.err.println(response.getStatus());
        for(HttpField header : response.getHeaders()){
            System.err.println(header.getName()+": "+header.getValue());
        }
        try(InputStream ins = listener.getInputStream()){
            IOUtil.readAll(ins,(bs,l)->{
                String b64 = Base64.encode(bs,l);
                System.err.println(b64);
            },buffer);
        }
    }
}
