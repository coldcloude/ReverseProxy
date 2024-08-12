package os.kai.rp.http.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.InetSocketAddress;

@Slf4j
public class HttpServer {
    private final Server server;
    public HttpServer(String host,int port,Handler handler){
        this(host,port,handler,null,null);
    }
    public HttpServer(String host,int port,Handler handler,String keystore,String keystorePassword){
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
            //ssl context factory
            SslContextFactory.Server factory = new SslContextFactory.Server();
            factory.setKeyStorePath(keystore);
            factory.setKeyStorePassword(keystorePassword);
            factory.setKeyManagerPassword(keystorePassword);
            //ssl http
            httpsConfig.setSecurePort(port);
            httpsConfig.setSecureScheme("https");
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            //ssl connector
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
        //handler
        server.setHandler(handler);
    }
    public void start() throws Exception {
        server.start();
    }
    public void startSync() {
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
}
