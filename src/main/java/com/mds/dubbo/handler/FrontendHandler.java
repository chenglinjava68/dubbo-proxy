package com.mds.dubbo.handler;

import com.mds.dubbo.codec.packet.Body;
import com.mds.dubbo.codec.packet.BodyHeartBeat;
import com.mds.dubbo.codec.packet.BodyRequest;
import com.mds.dubbo.codec.packet.DubboPacket;
import com.mds.dubbo.config.AppInfo;
import com.mds.dubbo.session.SessionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FrontendHandler extends ChannelInboundHandlerAdapter {

    private final List<AppInfo> appInfoList;

    public FrontendHandler(List<AppInfo> appInfoList) {
        this.appInfoList = appInfoList;
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
        appInfoList.forEach(appInfo -> connect(appInfo, inboundChannel));

    }

    private void connect(AppInfo appInfo, Channel inboundChannel) {
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop());
        b.option(ChannelOption.AUTO_READ, true)
                .channel(NettyEventLoopFactory.socketChannelClass())
                .handler(new BackendHandler(inboundChannel));
        ChannelFuture channelFuture = b.connect(appInfo.getIp(), appInfo.getPort());
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                inboundChannel.read();
                Channel channel = channelFuture.channel();
                log.info("in {} connect success out {}", inboundChannel.remoteAddress(), channel.localAddress());
                SessionManager sessionManager = SessionManager.getInstance();
                sessionManager.addConnection(appInfo, channel, inboundChannel);
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        log.info("channelRead  msg {}",msg);
        if (msg instanceof DubboPacket) {
            DubboPacket dubboPacket = (DubboPacket) msg;
            try {
                Body body = dubboPacket.getBody();
                if (body instanceof BodyRequest) {
                    BodyRequest bodyRequest = (BodyRequest) body;
                    String targetApplication = bodyRequest.getAttachments().get("target-application").toString();
                    SessionManager sessionManager = SessionManager.getInstance();
                    Channel outboundChannel = sessionManager.getConnection(targetApplication);
                    if (outboundChannel.isActive()) {
                        // 获取ByteBufAllocator用于创建新的ByteBuf
                        CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
                        compositeByteBuf.addComponent(true, dubboPacket.getDubboRequestHeader().getHeaderBytes());
                        compositeByteBuf.addComponent(true, dubboPacket.getBody().bytes());
                        outboundChannel.writeAndFlush(compositeByteBuf).addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                ctx.channel().read();
                            } else {
                                future.channel().close();
                            }
                        });
                    }
                } else if (body instanceof BodyHeartBeat) {
                    CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
                    compositeByteBuf.addComponent(true, dubboPacket.getDubboRequestHeader().getHeaderBytes());
                    log.debug("heartbeat:{}", ctx.channel());
                    ctx.writeAndFlush(compositeByteBuf).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.debug("write heartbeat success");
                            ctx.channel().read();
                        } else {
                            log.info("write heartbeat fail");
                            future.channel().close();
                        }
                    });;
                }
            } finally {
                dubboPacket.release();
            }

        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }
}
