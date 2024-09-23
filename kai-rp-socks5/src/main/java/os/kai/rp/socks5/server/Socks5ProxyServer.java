package os.kai.rp.socks5.server;

import lombok.extern.slf4j.Slf4j;
import os.kai.rp.TextProxyHub;
import os.kai.rp.server.TextProxyServer;
import os.kai.rp.socks5.Socks5Constant;
import os.kai.rp.socks5.Socks5Hub;
import os.kai.rp.socks5.client.Socks5ClientGroup;
import os.kai.rp.util.NettyUtil;

@Slf4j
public class Socks5ProxyServer {
    private final TextProxyServer proxyServer;
    private final Socks5Server socks5Server;
    public Socks5ProxyServer(String host,int port,String proxyHost,int proxyPort,int nop,long timeout){
        socks5Server = new Socks5Server(host,port);
        proxyServer = new TextProxyServer(proxyHost,proxyPort,nop,timeout,(sid,ctx)->{
            if(!sid.equals(Socks5Constant.SID)){
                String addr = NettyUtil.getRemoteAddress(ctx);
                Socks5Hub.get().registerProxy(addr,sid);
            }
            TextProxyHub.get().registerServerReceiver(sid,data->Socks5Hub.get().process(data));
        },(sid,ctx)->{
            if(!sid.equals(Socks5Constant.SID)){
                String addr = NettyUtil.getRemoteAddress(ctx);
                Socks5Hub.get().unregisterProxy(addr);
            }
            TextProxyHub.get().unregisterServerReceiver(sid);
        });
    }
    public void start(boolean useLocalAsDefault) throws Exception {
        //server for outside socks5 connect in
        Thread sock5Thread = new Thread(socks5Server::start);
        sock5Thread.start();
        //not proxy to client
        if(useLocalAsDefault){
            Socks5ClientGroup.start(Socks5Constant.SID);
        }
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
        int nop = Integer.parseInt(args[4]);
        long timeout = Long.parseLong(args[5]);
        boolean useLocalAsDefault = Integer.parseInt(args[6])>0;
//        String host = "127.0.0.1";
//        int port = 24466;
//        String proxyHost = "127.0.0.1";
//        int proxyPort = 13355;
//        int nop = 8;
//        long timeout = 30000L;
//        boolean useLocalAsDefault = true;
        Socks5ProxyServer server = new Socks5ProxyServer(host,port,proxyHost,proxyPort,nop,timeout);
        server.start(useLocalAsDefault);
    }
}
