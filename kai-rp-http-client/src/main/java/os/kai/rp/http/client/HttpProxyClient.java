package os.kai.rp.http.client;

import os.kai.rp.client.TextProxyClient;

public class HttpProxyClient {
    private final TextProxyClient proxyClient;
    private final HttpProxyClientSession proxyClientSession;
    public HttpProxyClient(String schema, String host, int port, int nop, String proxyHost, int proxyPort, String sessionId, long timeout) throws Exception {
        proxyClientSession = new HttpProxyClientSession(schema,host,port,sessionId,nop);
        proxyClient = new TextProxyClient(proxyHost,proxyPort,sessionId,timeout);
    }
    public void start(){
        proxyClient.startWithRetry(-1,5000L);
    }
    public static void main(String[] args) throws Exception{
        String schema = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        int nop = Integer.parseInt(args[3]);
        String proxyHost = args[4];
        int proxyPort = Integer.parseInt(args[5]);
        String sessionId = args[6];
        long timeout = Long.parseLong(args[7]);
//        String schema = "http";
//        String host = "mirrors.163.com";
//        int port = 80;
//        int nop = 20;
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        String sessionId = HttpConstant.SID;
//        long timeout = 30000L;
        HttpProxyClient client = new HttpProxyClient(schema,host,port,nop,proxyHost,proxyPort,sessionId,timeout);
        client.start();
    }
}
