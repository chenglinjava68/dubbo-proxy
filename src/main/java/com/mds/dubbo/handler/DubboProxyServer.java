package com.mds.dubbo.handler;


import com.mds.dubbo.ProxyServer;
import com.mds.dubbo.codec.DubboRequestDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;


@Component("dubboProxyServer")
@Slf4j
public class DubboProxyServer extends ProxyServer {

    @Override
    protected ChannelInitializer<Channel> channelInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast("dubbo-decoder",new DubboRequestDecoder());
                ch.pipeline().addLast("server-idle-handler", new IdleStateHandler(0, 0, 10000, TimeUnit.MILLISECONDS));
                ch.pipeline().addLast("session-handler", new SessionHandler());
                ch.pipeline().addLast("frontend-handler",new FrontendHandler(serverConfig.getBackend()));

            }
        };
    }
    @Override
    protected void config(ServerBootstrap b) {
        b.childOption(ChannelOption.AUTO_READ, false);
    }

}
