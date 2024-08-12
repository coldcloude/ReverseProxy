package os.kai.rp.http.client;

import org.eclipse.jetty.client.api.ContentProvider;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.util.AsyncProvider;
import os.kai.rp.util.Base64;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class HttpProxyBase64ContentProvider extends AsyncProvider<String,ByteBuffer> implements ContentProvider {
    private static class TransData implements Function<String,ByteBuffer> {
        private final byte[] buffer;
        private TransData(byte[] buffer) {
            this.buffer = buffer==null?new byte[HttpConstant.BUF_LEN]:buffer;
        }
        @Override
        public ByteBuffer apply(String data) {
            int len = Base64.decode(data,buffer);
            byte[] bs = new byte[len];
            System.arraycopy(buffer,0,bs,0,len);
            return ByteBuffer.wrap(bs);
        }
    }
    private final int length;
    public HttpProxyBase64ContentProvider(int length,byte[] buffer){
        super(new TransData(buffer));
        this.length = length;
    }
    public HttpProxyBase64ContentProvider(int length){
        this(length,null);
    }
    @Override
    public long getLength() {
        return length;
    }
}
