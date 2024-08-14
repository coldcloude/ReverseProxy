package os.kai.rp.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class TestBase64 {
    @Test
    public void test1() {
        String ss = "ReverseProxy-general.jpg";
        byte[] src = "ReverseProxy-general.jpg".getBytes(StandardCharsets.UTF_8);
        String str = Base64.encode(src);
        byte[] dst = Base64.decode(str);
        String ds = new String(dst,StandardCharsets.UTF_8);
        Assertions.assertEquals(ss,ds);
    }
    @Test
    public void test2() {
        byte[] dst = Base64.decode("");
        Assertions.assertEquals(dst.length,0);
    }
    @Test
    public void test3(){
        String src = "1234567890";
        String sb64 = Base64.encode(src.getBytes());
        byte[] buf = new byte[4];
        List<byte[]> rbbs = new LinkedList<>();
        Base64.decode(sb64,buf,(bs,len)->{
            System.err.println("base64 decoded: "+len);
            byte[] rbb = new byte[len];
            System.arraycopy(bs,0,rbb,0,len);
            rbbs.add(rbb);
        });
        String dst = new String(IOUtil.concat(rbbs));
        Assertions.assertEquals(src,dst);
    }
}
