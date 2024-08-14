package os.kai.rp.socks5;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import os.kai.rp.util.Base64;
import os.kai.rp.util.JacksonUtil;
import os.kai.rp.TextProxyHub;

import java.util.function.Consumer;

public class Socks5Util {
    private static void readAndSendRelay(String ssid,ByteBuf bb,byte[] buffer,Consumer<String> sendTo) throws JsonProcessingException {
        int readable;
        while((readable=bb.readableBytes())>0){
            int len = Math.min(readable,buffer.length);
            bb.readBytes(buffer,0,len);
            Socks5RelayEntity entity = new Socks5RelayEntity();
            entity.setSsid(ssid);
            entity.setData64(Base64.encode(buffer,len));
            String json = JacksonUtil.stringify(entity);
            sendTo.accept(Socks5Constant.PREFIX_RELAY+json);
        }
    }
    private static void sendToServer(String data){
        TextProxyHub.get().sendToServer(Socks5Constant.SID,data);
    }
    private static void sendToClient(String data){
        TextProxyHub.get().sendToClient(Socks5Constant.SID,data);
    }
    public static void readAndSendRelayToServer(String ssid, ByteBuf bb, byte[] buffer) throws JsonProcessingException {
        readAndSendRelay(ssid,bb,buffer,Socks5Util::sendToServer);
    }
    public static void readAndSendRelayToClient(String ssid, ByteBuf bb, byte[] buffer) throws JsonProcessingException {
        readAndSendRelay(ssid,bb,buffer,Socks5Util::sendToClient);
    }
}
