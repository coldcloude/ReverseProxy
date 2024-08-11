package os.kai.rp.http.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.InetSocketAddress;

@Slf4j
public class HttpProxyServer {

    private final Server server;

    public HttpProxyServer(String host, int port, Handler handler){
        this(host,port,handler,null,null);
    }

    public HttpProxyServer(String host, int port, Handler handler, String keystore, String keystorePassword){
        server = new Server();
        //host and port
        InetSocketAddress addr = host==null||host.isEmpty()?new InetSocketAddress(port):new InetSocketAddress(host,port);
        //off send
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSendServerVersion(false);
        httpsConfig.setSendDateHeader(false);
        httpsConfig.setSendXPoweredBy(false);
        //build connector
        ServerConnector connector;
        if(keystore==null||keystore.isEmpty()){
            connector = new ServerConnector(
                    server,
                    new HttpConnectionFactory(httpsConfig));
        }
        else {
            //配置SSL证书相关
            SslContextFactory.Server factory = new SslContextFactory.Server();
            factory.setKeyStorePath(keystore);
            factory.setKeyStorePassword(keystorePassword);
            factory.setKeyManagerPassword(keystorePassword);
            //SSL HTTP配置
            httpsConfig.setSecurePort(port);
            httpsConfig.setSecureScheme("https");
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            //SSL配置
            connector = new ServerConnector(
                    server,
                    new SslConnectionFactory(factory,HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig)
            );
        }
        //set connector
        connector.setHost(addr.getHostName());
        connector.setPort(addr.getPort());
        server.addConnector(connector);
        //servlet
        server.setHandler(handler);
    }

    public void start(){
        try{
            server.start();
            server.join();
        }
        catch(Exception e){
            log.warn("",e);
        }
        finally{
            server.destroy();
        }
    }

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        long timeout = Long.parseLong(args[2]);
        HttpProxyServer server = new HttpProxyServer(host,port,new HttpProxyHandler(timeout));
        server.start();
    }
}
