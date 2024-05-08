package com.mds.dubbo;

import com.mds.dubbo.config.AllocatorType;
import com.mds.dubbo.config.ServerConfig;
import com.mds.dubbo.core.NettyEventLoopFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class ProxyServer {
    @Autowired
    protected ServerConfig serverConfig;
    public static final EventLoopGroup serverBossGroup = NettyEventLoopFactory.eventLoopGroup(1, "boss");
    public static final EventLoopGroup serverWorkerGroup = NettyEventLoopFactory.eventLoopGroup(16, "worker") ;

    protected abstract ChannelInitializer<Channel> channelInitializer();

    protected void config(ServerBootstrap b) {
    }

    public void doStart() throws Exception {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(serverBossGroup, serverWorkerGroup)
                    .channel(NettyEventLoopFactory.serverSocketChannelClass())
                    .childHandler(channelInitializer())
            	    .handler(new LoggingHandler(LogLevel.INFO));
            b.childOption(ChannelOption.SO_RCVBUF, serverConfig.getReceiveBuffer())
                    .childOption(ChannelOption.SO_SNDBUF, serverConfig.getSendBuffer());
            if (serverConfig.getAllocatorType() == AllocatorType.Pooled) {
                b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            }
            if (serverConfig.getAllocatorType() == AllocatorType.Unpooled) {
                b.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
            }
            config(b);
            b.bind(serverConfig.getServerIp(), serverConfig.getServerPort())
                    .addListener(future -> log.info("{} Started with config: {}", getClass().getSimpleName(), serverConfig))
                    .sync().channel().closeFuture().sync();
        } finally {
            serverBossGroup.shutdownGracefully();
            serverWorkerGroup.shutdownGracefully();
        }
    }
}
