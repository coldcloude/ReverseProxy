package os.kai.rp.socks5.client;

import os.kai.rp.client.TextProxyClient;
import os.kai.rp.socks5.Socks5Constant;

public class Socks5ProxyClient {
    private final TextProxyClient proxyClient;
    private final Socks5ClientGroup clientGroup;
    public Socks5ProxyClient(String proxyHost,int proxyPort,String sessionId,long timeout) {
        clientGroup = new Socks5ClientGroup(sessionId);
        proxyClient = new TextProxyClient(proxyHost,proxyPort,sessionId,timeout);
    }
    public void start() throws Exception{
        clientGroup.start();
        proxyClient.startWithRetry(-1,5000L);
    }
    public static void main(String[] args) throws Exception{
        String proxyHost = args[0];
        int proxyPort = Integer.parseInt(args[1]);
        String sessionId = args[2];
        long timeout = Long.parseLong(args[3]);
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        long timeout = 30000L;
        Socks5ProxyClient client = new Socks5ProxyClient(proxyHost,proxyPort,sessionId,timeout);
        client.start();
    }
}
