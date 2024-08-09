package os.kai.rp.client;

import os.kai.rp.ProxyHub;

public class TestClient {
    public static void main(String[] args) throws Exception {
        String sid = "abc";
        new Thread(()->{
            ProxyClient client = new ProxyClient("127.0.0.1",13355,sid,30000L);
            client.startWithRetry(-1,5000L);
        }).start();
        ProxyHub.get().registerClientReceiver(sid,data->{
            ProxyHub.get().sendToServer(sid,"echo: "+data);
        });
    }
}
