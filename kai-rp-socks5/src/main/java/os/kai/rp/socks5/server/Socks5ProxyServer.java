package os.kai.rp.socks5.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.server.TextProxyServer;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.client.Socks5ClientGroup;
import os.kai.rp.util.NettyUtil;

import java.net.InetSocketAddress;

@Slf4j
public class Socks5ProxyServer {
    private final TextProxyServer proxyServer;
    private final Socks5Server socks5Server;
    private final Socks5ClientGroup clientGroup;
    public Socks5ProxyServer(String host,int port,String proxyHost,int proxyPort,long timeout){
        socks5Server = new Socks5Server(host,port);
        clientGroup = new Socks5ClientGroup(Socks5Constant.SID);
        proxyServer = new TextProxyServer(proxyHost,proxyPort,timeout,(sid,ctx)->{
            String addr = NettyUtil.getRemoteAddress(ctx);
            Socks5Hub.get().registerProxy(addr,sid);
            TextProxyHub.get().registerServerReceiver(sid,data->Socks5Hub.get().process(data));
        },(sid,ctx)->{
            String addr = NettyUtil.getRemoteAddress(ctx);
            Socks5Hub.get().unregisterProxy(addr);
            TextProxyHub.get().unregisterServerReceiver(addr);
        });
    }
    public void start() throws Exception {
        //server for outside socks5 connect in
        Thread sock5Thread = new Thread(socks5Server::start);
        sock5Thread.start();
        //not proxy to client
        clientGroup.start();
        //proxy to client
        Thread proxyThread = new Thread(proxyServer::start);
        proxyThread.start();
        sock5Thread.join();
        proxyThread.join();
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
        Socks5ProxyServer server = new Socks5ProxyServer(host,port,proxyHost,proxyPort,timeout);
        server.start();
    }
}
