package os.kai.rp.http.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.http.client.HttpProxyBase64ContentProvider;
import os.kai.rp.util.Base64;
import os.kai.rp.util.IOUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class TestClient2 {
    public static void main(String[] args) throws Exception {
        byte[] buffer = new byte[HttpConstant.BUF_LEN];
        HttpClient client = new HttpClient();
        client.start();
        client.getContentDecoderFactories().clear();
        Request request = client.newRequest("http://mirrors.163.com/");
        request.method("GET");
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);
        Response response = listener.get(30000L,TimeUnit.MILLISECONDS);
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
