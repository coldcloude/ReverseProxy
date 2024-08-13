package os.kai.rp.http.client;

import os.kai.rp.client.TextProxyClient;
import os.kai.rp.http.HttpConstant;

public class HttpProxyClient {
    private final TextProxyClient proxyClient;
    private final HttpProxyClientSession proxyClientSession;
    public HttpProxyClient(String schema, String host, int port, String proxyHost, int proxyPort, String sessionId, long timeout) throws Exception {
        proxyClientSession = new HttpProxyClientSession(schema,host,port,sessionId,timeout);
        proxyClient = new TextProxyClient(proxyHost,proxyPort,sessionId,timeout);
    }
    public void start() throws Exception{
        proxyClientSession.start();
        proxyClient.startWithRetry(-1,5000L);
    }
    public static void main(String[] args) throws Exception{
        String schema = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String proxyHost = args[3];
        int proxyPort = Integer.parseInt(args[4]);
        String sessionId = args[5];
        long timeout = Long.parseLong(args[6]);
//        String schema = "https";
//        String host = "fed595f675edb37da0.gradio.live";
//        int port = 443;
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        String sessionId = HttpConstant.SID;
//        long timeout = 30000L;
        HttpProxyClient client = new HttpProxyClient(schema,host,port,proxyHost,proxyPort,sessionId,timeout);
        client.start();
    }
}
