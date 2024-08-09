package os.kai.rp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ProxyHub {

    private static volatile ProxyHub hub = null;

    public static ProxyHub get(){
        if(hub==null){
            synchronized(ProxyHub.class){
                if(hub==null){
                    hub = new ProxyHub();
                }
            }
        }
        return hub;
    }

    private final Map<String,Consumer<String>> serverReceiverMap = new ConcurrentHashMap<>();

    private final Map<String,Consumer<String>> clientReceiverMap = new ConcurrentHashMap<>();

    public void removeClientReceiver(String sid){
        clientReceiverMap.remove(sid);
    }

    public void registerClientReceiver(String sid,Consumer<String> receiver){
        clientReceiverMap.put(sid,receiver);
    }

    public void removeServerReceiver(String sid){
        serverReceiverMap.remove(sid);
    }

    public void registerServerReceiver(String sid,Consumer<String> receiver){
        serverReceiverMap.put(sid,receiver);
    }

    public void sendToClient(String sid, String data){
        Consumer<String> receiver = clientReceiverMap.get(sid);
        if(receiver!=null){
            receiver.accept(data);
        }
    }

    public void sendToServer(String sid, String data){
        Consumer<String> receiver = serverReceiverMap.get(sid);
        if(receiver!=null){
            receiver.accept(data);
        }
    }
}
