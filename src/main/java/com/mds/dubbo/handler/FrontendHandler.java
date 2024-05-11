package com.mds.dubbo.handler;
import com.mds.dubbo.codec.packet.Body;
import com.mds.dubbo.codec.packet.BodyRequest;
import com.mds.dubbo.codec.packet.DubboPacket;
import com.mds.dubbo.config.AppInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class FrontendHandler extends ChannelInboundHandlerAdapter {

    private final List<AppInfo> appInfoList;
    private Channel outboundChannel;

    private static final Map<String,Channel> channelMap = new ConcurrentHashMap<>(16);

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
        for (AppInfo appInfo : appInfoList) {
            Bootstrap b = new Bootstrap();
            b.group(inboundChannel.eventLoop());
            b.option(ChannelOption.AUTO_READ, true)
                    .channel(NettyEventLoopFactory.socketChannelClass())
                    .handler(new BackendHandler(inboundChannel));
            ChannelFuture f = b.connect(appInfo.getIp(), appInfo.getPort()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("remote {} connect success", ctx.channel().remoteAddress());
                    inboundChannel.read();
                } else {
                    inboundChannel.close();
                }
            });
            outboundChannel = f.channel();
            channelMap.put(appInfo.getName(),outboundChannel);
        }
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
                    if (outboundChannel.isActive()) {
                        // 获取ByteBufAllocator用于创建新的ByteBuf
                        ByteBufAllocator alloc = ctx.alloc();

                        // 创建一个新的ByteBuf，大小为两个原始ByteBuf大小之和
                        ByteBuf bytes = dubboPacket.getBody().bytes();
                        int totalLength = dubboPacket.getDubboRequestHeader().getHeaderBytes().readableBytes() +
                                bytes.readableBytes();
                        ByteBuf combinedByteBuf = alloc.buffer(totalLength);

                        // 将DubboRequestHeader中的ByteBuf内容复制到新的ByteBuf中
                        combinedByteBuf.writeBytes(dubboPacket.getDubboRequestHeader().getHeaderBytes());

                        // 将Body中的ByteBuf内容复制到新的ByteBuf中
                        combinedByteBuf.writeBytes(dubboPacket.getBody().bytes());
                        outboundChannel.writeAndFlush(combinedByteBuf).addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                ctx.channel().read();
                            } else {
                                future.channel().close();
                            }
                        });
                    }
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
