package com.mds.dubbo.handler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FrontendHandler extends ChannelInboundHandlerAdapter {

    private final String remoteHost;
    private final int remotePort;
    private Channel outboundChannel;

    private final Map<String,Channel> channelMap = new ConcurrentHashMap<>(16);

    public FrontendHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("channel is active: {}", ctx.channel().remoteAddress());
        Channel inboundChannel = ctx.channel();
        if(channelMap.containsKey(remoteHost)){
            outboundChannel = channelMap.get(remoteHost);
        } else {
          Bootstrap b = new Bootstrap();
          b.group(inboundChannel.eventLoop());
          b.option(ChannelOption.AUTO_READ, true)
                .channel(NettyEventLoopFactory.socketChannelClass())
                .handler(new BackendHandler(inboundChannel));
         ChannelFuture f = b.connect(remoteHost, remotePort).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("remote {} connect success", ctx.channel().remoteAddress());
                inboundChannel.read();
            } else {
                inboundChannel.close();
            }
          });
          outboundChannel = f.channel();
          channelMap.put(remoteHost,outboundChannel);
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        log.info("channelRead  msg {}",msg);
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }
}
