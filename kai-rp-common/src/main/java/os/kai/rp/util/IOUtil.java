package os.kai.rp.util;

import java.io.IOException;
import java.io.InputStream;

public class IOUtil {
    @FunctionalInterface
    public interface BufferConsumer {
        void accept(byte[] buf, int len) throws IOException;
    }
    public static void readAll(InputStream ins,BufferConsumer op,byte[] buf) throws IOException {
        int len;
        while((len=ins.read(buf,0,buf.length))>=0){
            if(len>0){
                op.accept(buf,len);
            }
        }
    }
}
