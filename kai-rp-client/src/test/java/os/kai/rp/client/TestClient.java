package os.kai.rp.client;

import os.kai.rp.TextProxyHub;

public class TestClient {
    public static void main(String[] args) throws Exception {
        String sid = "abc";
        new Thread(()->{
            TextProxyClient client = new TextProxyClient("127.0.0.1",13355,sid,30000L);
            client.startWithRetry(-1,5000L);
        }).start();
        TextProxyHub.get().registerClientReceiver(sid,data->{
            TextProxyHub.get().sendToServer(sid,"echo: "+data);
        });
    }
}
