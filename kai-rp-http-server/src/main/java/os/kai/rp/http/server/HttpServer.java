package os.kai.rp.http.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.ChainedChannelInitializer;
import os.kai.rp.NettyServer;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class HttpServer {
    private final HttpProxyServerSession session;
    private final NettyServer server;
    public HttpServer(String host,int port,String sessionId){
        this(host,port,sessionId,null,null);
    }
    public HttpServer(String host,int port,String sessionId,String keystore,String keystorePassword){
        session = new HttpProxyServerSession(sessionId);
        List<Function<SocketChannel,ChannelHandler>> handlers = new LinkedList<>();
        //https
        if(keystore!=null&&keystorePassword!=null){
            handlers.add(ch->{
                try{
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    try(FileInputStream ins = new FileInputStream(keystore)){
                        keyStore.load(ins,keystorePassword.toCharArray());
                    }
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
                    SslContext context = SslContextBuilder.forServer(keyManagerFactory).build();
                    return context.newHandler(ch.alloc());
                }
                catch(Exception e){
                    log.warn("init ssl failed.",e);
                    return null;
                }
            });
        }
        //http encoder and decoder
        handlers.add(ch->new HttpServerCodec());
        //underlying handler
        handlers.add(ch->new HttpProxyClientHandler(session));
        //build
        ChainedChannelInitializer initializer = new ChainedChannelInitializer(handlers);
        server = new NettyServer(initializer,host,port);
    }
    public ChannelFuture startAsync(){
        return server.startAsync();
    }
    public void start(){
        server.start();
    }
}
