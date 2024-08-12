package os.kai.rp.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
}
