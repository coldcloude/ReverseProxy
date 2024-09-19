package os.kai.rp.http;

import io.netty.handler.codec.http.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class HttpRequestEntity {
    private String hsid = "";
    private String version = "";
    private String method = "";
    private String path = "";
    private Map<String,String> headers = new LinkedHashMap<>();
    public HttpRequestEntity(String hsid, HttpRequest request){
        this.hsid = hsid;
        version = request.protocolVersion().text();
        method = request.method().name();
        path = request.uri();
        for(Map.Entry<String,String> kv : request.headers()){
            headers.put(kv.getKey(),kv.getValue());
        }
    }
    public DefaultHttpRequest output(){
        HttpVersion version = HttpVersion.valueOf(this.version);
        HttpMethod method = HttpMethod.valueOf(this.method);
        DefaultHttpRequest request = new DefaultHttpRequest(version,method,path);
        HttpHeaders headers = request.headers();
        for(Map.Entry<String,String> kv : this.headers.entrySet()){
            headers.set(kv.getKey(),kv.getValue());
        }
        return request;
    }
    public void display(Logger log){
        log.info(hsid+" request: "+version+" "+method+" "+path);
        if(log.isDebugEnabled()){
            for(Map.Entry<String,String> kv : headers.entrySet()){
                log.debug(hsid+" request: [header] "+kv.getKey()+": "+kv.getValue());
            }
        }
    }
}
