package com.beaver.rpc.core.transport.impl.netty.client;

import com.beaver.rpc.core.transport.Client;
import com.beaver.rpc.core.transport.impl.netty.codec.RpcMessageDecoder;
import com.beaver.rpc.core.transport.impl.netty.codec.RpcMessageEncoder;
import com.beaver.rpc.common.domain.RequestRepository;
import com.beaver.rpc.common.enums.CompressTypeEnum;
import com.beaver.rpc.common.enums.SerializationTypeEnum;
import com.beaver.rpc.common.constant.RpcConstants;
import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.domain.RpcResponse;
import com.beaver.rpc.common.domain.RpcMessage;
import com.beaver.rpc.common.util.SingletonFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient implements Client {

    // or we can use a channel pool
    private Channel channel;
    private RequestRepository requestRepository;
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private InetSocketAddress serviceAddress;

    public Channel getChannel() {
        return channel;
    }

    public NettyClient() {
        this.requestRepository = SingletonFactory.getInstance(RequestRepository.class);
    }

    public NettyClient(InetSocketAddress serviceAddress) {
        // initialize resources such as EventLoopGroup, Bootstrap
        this.eventLoopGroup = new NioEventLoopGroup();
        this.requestRepository = SingletonFactory.getInstance(RequestRepository.class);
        this.serviceAddress = serviceAddress;
        openClient();
    }

    @Override
    public boolean openClient() {
        NettyClient client = this;
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  The timeout period of the connection.
                //  If this time is exceeded or the connection cannot be established, the connection fails.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // If no data is sent to the server within 15 seconds, a heartbeat request is sent
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyClientHandler(client));
                    }
                });
        return true;
    }

    @Override
    public boolean closeClient() {
        return false;
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException {
        // build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // connect remote server if needed
        if (channel == null || !channel.isActive()) {
            doConnect(serviceAddress);
        }

        if (channel.isActive()) {
            // put unprocessed request
            requestRepository.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .serializer(SerializationTypeEnum.PROTOSTUFF.getCode())
                    .compressor(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            ChannelFuture channelFuture = channel.writeAndFlush(rpcMessage);


            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }

        return resultFuture;
    }

    @Override
    public boolean processResponse(RpcResponse rpcResponse) {
        requestRepository.complete(rpcResponse);
        return false;
    }

    private void doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        this.channel = completableFuture.get();
    }

}
