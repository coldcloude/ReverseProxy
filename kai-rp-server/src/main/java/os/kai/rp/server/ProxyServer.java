package os.kai.rp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import os.kai.rp.LineBasedChannelInitializer;

@Slf4j
public class ProxyServer {

    private final String host;

    private final int port;

    private final long timeout;

    public ProxyServer(String host,int port,long timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    public void start(){

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {

            log.info("-----> start server: port="+port);

            ChannelFuture channelFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new LineBasedChannelInitializer(()->new ProxyClientHandler(timeout)))
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,false)
                    .bind(host,port)
                    .sync();

            channelFuture
                    .channel()
                    .closeFuture()
                    .sync();

        } catch (Exception e) {
            log.error("-----> server start fail.",e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
