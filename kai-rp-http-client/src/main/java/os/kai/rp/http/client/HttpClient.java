package os.kai.rp.http.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.ChainedChannelInitializer;
import os.kai.rp.NettyClient;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class HttpClient {
    private final NettyClient client;
    public HttpClient(String schema,String host,int port,String hsid,NioEventLoopGroup group,HttpProxyClientSession session) {
        List<Function<SocketChannel,ChannelHandler>> handlers = new LinkedList<>();
        //https
        if(schema.equals("https")){
            handlers.add(ch->{
                try{
                    SslContext context = SslContextBuilder.forClient().build();
                    return context.newHandler(ch.alloc());
                }
                catch(Exception e){
                    log.warn("init ssl failed.",e);
                    return null;
                }
            });
        }
        //http encoder and decoder
        handlers.add(ch->new HttpClientCodec());
        //underlying handler
        handlers.add(ch->new HttpProxyServerHandler(host,port,session,hsid));
        //build
        ChainedChannelInitializer initializer = new ChainedChannelInitializer(handlers);
        client = new NettyClient(initializer,host,port,group);
    }
    public ChannelFuture startAsync(){
        return client.startAsync();
    }
}
