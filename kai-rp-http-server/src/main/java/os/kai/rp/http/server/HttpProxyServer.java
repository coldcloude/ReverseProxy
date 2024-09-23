package os.kai.rp.http.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.http.HttpConstant;
import os.kai.rp.server.TextProxyServer;

@Slf4j
public class HttpProxyServer {
    private final TextProxyServer proxyServer;
    private final HttpServer httpServer;
    public HttpProxyServer(String host, int port, String proxyHost, int proxyPort, int nop, long timeout){
        this(host,port,proxyHost,proxyPort,nop,timeout,null,null);
    }
    public HttpProxyServer(String host, int port, String proxyHost, int proxyPort, int nop, long timeout, String keystore, String keystorePassword){
        httpServer = new HttpServer(host,port,HttpConstant.SID,keystore,keystorePassword);
        proxyServer = new TextProxyServer(proxyHost,proxyPort,nop,timeout);
    }
    public void start(){
        httpServer.startAsync();
        proxyServer.start();
    }
    public static void main(String[] args){
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String proxyHost = args[2];
        int proxyPort = Integer.parseInt(args[3]);
        int nop = Integer.parseInt(args[4]);
        long timeout = Long.parseLong(args[5]);
//        String host = "127.0.0.1";
//        int port = 24466;
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        int nop = 2;
//        long timeout = 30000L;
        HttpProxyServer server = new HttpProxyServer(host,port,proxyHost,proxyPort,nop,timeout);
        server.start();
    }
}
