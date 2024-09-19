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
public class HttpResponseEntity {
    private String hsid = "";
    private String version = "";
    private int status = 0;
    private Map<String,String> headers = new LinkedHashMap<>();
    public HttpResponseEntity(String hsid, HttpResponse response){
        this.hsid = hsid;
        version = response.protocolVersion().text();
        status = response.status().code();
        for(Map.Entry<String,String> kv : response.headers()){
            headers.put(kv.getKey(),kv.getValue());
        }
    }
    public DefaultHttpResponse output(){
        HttpVersion version = HttpVersion.valueOf(this.version);
        HttpResponseStatus status = HttpResponseStatus.valueOf(this.status);
        DefaultHttpResponse response = new DefaultHttpResponse(version,status);
        HttpHeaders headers = response.headers();
        for(Map.Entry<String,String> kv : this.headers.entrySet()){
            headers.set(kv.getKey(),kv.getValue());
        }
        return response;
    }
    public void display(Logger log){
        log.info(hsid+" response: "+version+" "+status);
        if(log.isDebugEnabled()){
            for(Map.Entry<String,String> kv : headers.entrySet()){
                log.debug(hsid+" response: [header] "+kv.getKey()+": "+kv.getValue());
            }
        }
    }
}
