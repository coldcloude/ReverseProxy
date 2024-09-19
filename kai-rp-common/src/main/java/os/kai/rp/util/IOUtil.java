package os.kai.rp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class IOUtil {
    private IOUtil(){

    }
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
    public static byte[] concat(List<byte[]> bbs){
        int sum = 0;
        for(byte[] bb : bbs){
            sum += bb.length;
        }
        byte[] r = new byte[sum];
        int offset = 0;
        for(byte[] bb : bbs){
            System.arraycopy(bb,0,r,offset,bb.length);
            offset += bb.length;
        }
        return r;
    }
}
