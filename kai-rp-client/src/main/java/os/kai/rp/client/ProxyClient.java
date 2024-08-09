package os.kai.rp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.LineBasedChannelInitializer;

@Slf4j
public class ProxyClient {

    private final String host;

    private final int port;

    private final Bootstrap bootstrap;

    public ProxyClient(String host,int port,String sessionId,long timeout) {
        this.host = host;
        this.port = port;
        bootstrap = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE,false)
                .handler(new LineBasedChannelInitializer(()->new ProxyServerHandler(sessionId,timeout)));
    }

    public void startWithRetry(int maxRetry, long interval){
        int retry = Math.max(0,maxRetry);
        while(retry>=0){
            Thread.interrupted();
            try{
                log.info("connecting to "+host+":"+port+"...");
                ChannelFuture cf = bootstrap.connect(host,port);
                cf.channel().closeFuture().sync();
                log.info("connection closed, retry...");
                if(maxRetry>=0){
                    retry--;
                }
                Thread.sleep(interval);
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }
}
