package os.kai.rp.http.client;

import org.eclipse.jetty.client.api.ContentProvider;
import os.kai.rp.Base64AsyncProvider;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.util.AsyncProvider;
import os.kai.rp.util.Base64;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.Function;

public class HttpProxyBase64ContentProvider implements ContentProvider {
    private final Base64AsyncProvider provider = new Base64AsyncProvider();
    private final Iterator<ByteBuffer> iterator = new Iterator<ByteBuffer>() {
        @Override
        public boolean hasNext() {
            return provider.iterator().hasNext();
        }
        @Override
        public ByteBuffer next() {
            byte[] bs = provider.iterator().next();
            return ByteBuffer.wrap(bs);
        }
    };;
    private final int length;
    public HttpProxyBase64ContentProvider(int length){
        this.length = length;
    }
    @Override
    public long getLength() {
        return length;
    }
    @Override
    public Iterator<ByteBuffer> iterator() {
        return iterator;
    }
    public void provide(String data){
        provider.provide(data);
    }
    public void finish(){
        provider.finish();
    }
}
