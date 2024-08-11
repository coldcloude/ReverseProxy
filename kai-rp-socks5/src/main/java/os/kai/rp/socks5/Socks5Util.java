package os.kai.rp.socks5;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import os.kai.rp.util.Base64;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.TextProxyHub;

public class Socks5Util {
    public static void readAndSendRelay(String ssid, ByteBuf bb, byte[] buffer) throws JsonProcessingException {
        int readable;
        while((readable=bb.readableBytes())>0){
            int len = Math.min(readable,buffer.length);
            bb.readBytes(buffer,0,len);
            Socks5RelayEntity entity = new Socks5RelayEntity();
            entity.setSsid(ssid);
            entity.setData64(Base64.encode(buffer,len));
            String json = JacksonUtil.stringify(entity);
            TextProxyHub.get().sendToClient(Socks5Constant.SID,Socks5Constant.PREFIX_RELAY+json);
        }
    }
}
