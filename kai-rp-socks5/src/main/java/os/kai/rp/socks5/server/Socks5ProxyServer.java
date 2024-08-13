package os.kai.rp.socks5.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.server.TextProxyServer;

@Slf4j
public class Socks5ProxyServer {
    private final TextProxyServer proxyServer;
    private final Socks5Server socks5Server;
    public Socks5ProxyServer(String host,int port,String proxyHost,int proxyPort,long timeout){
        socks5Server = new Socks5Server(host,port);
        proxyServer = new TextProxyServer(proxyHost,proxyPort,timeout);
    }
    public void start() throws Exception {
        Thread sock5Thread = new Thread(socks5Server::start);
        sock5Thread.start();
        Thread proxyThread = new Thread(proxyServer::start);
        proxyThread.start();
        sock5Thread.join();
        proxyThread.join();
    }
    public static void main(String[] args) throws Exception{
//        String host = args[0];
//        int port = Integer.parseInt(args[1]);
//        String proxyHost = args[2];
//        int proxyPort = Integer.parseInt(args[3]);
//        long timeout = Long.parseLong(args[4]);
        String host = "127.0.0.1";
        int port = 24466;
        String proxyHost = "127.0.0.1";
        int proxyPort = 13355;
        long timeout = 30000L;
        Socks5ProxyServer server = new Socks5ProxyServer(host,port,proxyHost,proxyPort,timeout);
        server.start();
    }
}
