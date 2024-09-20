package os.kai.rp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer {
    private final ChainedChannelInitializer initializer;

    private final String host;

    private final int port;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyServer(ChainedChannelInitializer initializer, String host, int port) {
        this.initializer = initializer;
        this.host = host;
        this.port = port;
    }

    public ChannelFuture startAsync(){
        log.info("-----> start server: port="+port);
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(initializer)
                .option(ChannelOption.SO_BACKLOG,128)
                .option(ChannelOption.SO_REUSEADDR,true)
                .childOption(ChannelOption.SO_KEEPALIVE,false)
                .bind(host,port);
    }

    public void start(){
        try {
            ChannelFuture channelFuture = startAsync();
            channelFuture = channelFuture.sync();
            channelFuture
                    .channel()
                    .closeFuture()
                    .sync();
        }
        catch (Exception e) {
            log.error("-----> server start fail.",e);
        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
