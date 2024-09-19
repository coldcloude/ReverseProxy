package os.kai.rp.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import os.kai.rp.util.Base64;
import os.kai.rp.util.NettyUtil;

@Getter
@Setter
@NoArgsConstructor
public class HttpPayloadEntity {
    private String hsid = "";
    private String data64 = "";
    private int last = 0;
    public HttpPayloadEntity(String hsid,HttpContent content){
        this.hsid = hsid;
        ByteBuf bb = content.content();
        byte[] buf = NettyUtil.readBytes(bb);
        data64 = Base64.encode(buf);
        last = content instanceof LastHttpContent?1:0;
    }
    public DefaultHttpContent output(){
        byte[] bs = Base64.decode(data64);
        ByteBuf bb = Unpooled.wrappedBuffer(bs);
        return last>0?new DefaultLastHttpContent(bb):new DefaultHttpContent(bb);
    }
    public void display(Logger log){
        if(log.isDebugEnabled()){
            int b64len = data64.length();
            StringBuilder builder = new StringBuilder();
            builder.append(hsid).append(" payload: [").append(Base64.size(data64)).append("] ");
            if(last>0){
                builder.append("(last) ");
            }
            if(b64len<=36){
                builder.append(data64);
            }
            else {
                builder.append(data64,0,16).append("...").append(data64,b64len-16,b64len);
            }
            log.debug(builder.toString());
        }
    }
}
