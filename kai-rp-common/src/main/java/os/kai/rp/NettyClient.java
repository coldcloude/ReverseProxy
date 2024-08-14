package os.kai.rp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClient {

    private final String host;

    private final int port;

    private final Bootstrap bootstrap;

    public NettyClient(ChainedChannelInitializer initializer, String host, int port, NioEventLoopGroup group) {
        this.host = host;
        this.port = port;
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE,false)
                .handler(initializer);
    }

    public NettyClient(ChainedChannelInitializer initializer, String host, int port) {
        this(initializer,host,port,new NioEventLoopGroup());
    }

    public ChannelFuture startAsync(){
        log.info("connecting to "+host+":"+port+"...");
        return bootstrap.connect(host,port);
    }

    public void startWithRetry(int maxRetry, long interval){
        int retry = Math.max(0,maxRetry);
        while(retry>=0){
            if(!Thread.interrupted()){
                try{
                    ChannelFuture cf = startAsync();
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
}
