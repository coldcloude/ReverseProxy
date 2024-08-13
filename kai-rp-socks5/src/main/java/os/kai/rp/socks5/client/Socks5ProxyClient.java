package os.kai.rp.socks5.client;

import os.kai.rp.client.TextProxyClient;
import os.kai.rp.socks5.Socks5Constant;

public class Socks5ProxyClient {
    private final TextProxyClient proxyClient;
    public Socks5ProxyClient(String proxyHost,int proxyPort,String sessionId,long timeout) {
        proxyClient = new TextProxyClient(proxyHost,proxyPort,sessionId,timeout);
    }
    public void start() throws Exception{
        Socks5ClientGroup.get().start();
        proxyClient.startWithRetry(-1,5000L);
    }
    public static void main(String[] args) throws Exception{
//        String proxyHost = args[0];
//        int proxyPort = Integer.parseInt(args[1]);
//        long timeout = Long.parseLong(args[2]);
        String proxyHost = "127.0.0.1";
        int proxyPort = 13355;
        long timeout = 30000L;
        Socks5ProxyClient client = new Socks5ProxyClient(proxyHost,proxyPort,Socks5Constant.SID,timeout);
        client.start();
    }
}
