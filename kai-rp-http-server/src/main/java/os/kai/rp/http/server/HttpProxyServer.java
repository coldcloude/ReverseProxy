package os.kai.rp.http.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.server.TextProxyServer;

@Slf4j
public class HttpProxyServer {
    private final TextProxyServer proxyServer;
    private final HttpServer httpServer;
    public HttpProxyServer(String host, int port, String proxyHost, int proxyPort, long timeout){
        this(host,port,proxyHost,proxyPort,timeout,null,null);
    }
    public HttpProxyServer(String host, int port, String proxyHost, int proxyPort, long timeout, String keystore, String keystorePassword){
        httpServer = new HttpServer(host,port,new HttpProxyHandler(timeout),keystore,keystorePassword);
        proxyServer = new TextProxyServer(proxyHost,proxyPort,timeout);
    }
    public void start() throws Exception {
        httpServer.start();
        proxyServer.start();
    }
    public static void main(String[] args) throws Exception{
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String proxyHost = args[2];
        int proxyPort = Integer.parseInt(args[3]);
        long timeout = Long.parseLong(args[4]);
//        String host = "127.0.0.1";
//        int port = 24466;
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        long timeout = 30000L;
        HttpProxyServer server = new HttpProxyServer(host,port,proxyHost,proxyPort,timeout);
        server.start();
    }
}
