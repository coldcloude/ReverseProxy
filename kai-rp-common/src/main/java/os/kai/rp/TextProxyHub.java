package os.kai.rp;

import os.kai.rp.util.DoubleLockSingleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TextProxyHub {

    private static final DoubleLockSingleton<TextProxyHub> hub = new DoubleLockSingleton<>(TextProxyHub::new);

    public static TextProxyHub get(){
        return hub.get();
    }

    private final Map<String,Consumer<String>> serverReceiverMap = new ConcurrentHashMap<>();

    private final Map<String,Consumer<String>> clientReceiverMap = new ConcurrentHashMap<>();

    public void unregisterClientReceiver(String sid){
        clientReceiverMap.remove(sid);
    }

    public void registerClientReceiver(String sid,Consumer<String> receiver){
        clientReceiverMap.put(sid,receiver);
    }

    public void unregisterServerReceiver(String sid){
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
