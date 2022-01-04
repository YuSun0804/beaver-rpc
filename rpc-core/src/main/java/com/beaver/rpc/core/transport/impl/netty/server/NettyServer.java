package com.beaver.rpc.core.transport.impl.netty.server;

import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.core.transport.Server;
import com.beaver.rpc.core.transport.impl.netty.codec.RpcMessageDecoder;
import com.beaver.rpc.core.transport.impl.netty.codec.RpcMessageEncoder;
import com.beaver.rpc.common.constant.RpcConstants;
import com.beaver.rpc.common.util.RuntimeUtil;
import com.beaver.rpc.common.util.ThreadPoolFactoryUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import lombok.extern.slf4j.Slf4j;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyServer implements Server {
    private volatile boolean isStarted;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup serviceHandlerGroup;
    private ServerBootstrap bootstrap;
    private ServerChannel serverChannel;

    public ConcurrentMap<String, Channel> getClientChannels() {
        return clientChannels;
    }

    private ConcurrentMap<String, Channel> clientChannels;

    @Override
    public boolean startServer() {
        if (isStarted) {
            return true;
        }

        clientChannels = new ConcurrentHashMap<>();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtils.createThreadFactory("service-handler-group", false)
        );
        NettyServer nettyServer = this;
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new NettyServerHandler(nettyServer));
                        }
                    });

            // 绑定端口，同步等待绑定成功
            ChannelFuture channelFuture = bootstrap.bind(host, RpcConstants.NETTY_SERVICE_PORT);
            channelFuture.syncUninterruptibly();
            serverChannel = (ServerChannel) channelFuture.channel();
            isStarted = true;
        } catch (UnknownHostException e) {
            log.error("unknown host when start server:", e);
        }
        return true;
    }

    @Override
    public boolean stopServer() {
        if (isStarted) {
            log.info("shutdown bossGroup and workerGroup");
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                serviceHandlerGroup.shutdownGracefully();
            }

            if (serverChannel != null) {
                ChannelFuture future = serverChannel.close();
                future.addListener((ChannelFutureListener) future1 -> {
                    if (!future1.isSuccess()) {
                        log.warn("Netty ServerChannel[{}] close failed", future1.cause());
                    }
                });
            }

            for (Channel channel : clientChannels.values()) {
                channel.close();
            }
            isStarted = false;
        }

        return true;
    }

    @Override
    public boolean addService(RpcService rpcServiceConfig) {
        return false;
    }

    @Override
    public boolean removeService(RpcService rpcServiceConfig) {
        return false;
    }

    @Override
    public boolean processRequest() {
        return false;
    }
}
