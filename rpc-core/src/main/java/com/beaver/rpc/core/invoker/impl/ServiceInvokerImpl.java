package com.beaver.rpc.core.invoker.impl;

import com.beaver.rpc.common.extension.ExtensionLoader;
import com.beaver.rpc.core.registry.ServiceChangeCallback;
import com.beaver.rpc.core.registry.ServiceRegistry;
import com.beaver.rpc.core.transport.Client;
import com.beaver.rpc.core.transport.impl.netty.client.NettyClient;
import com.beaver.rpc.core.transport.impl.socket.client.SocketClient;
import com.beaver.rpc.common.enums.RpcErrorMessageEnum;
import com.beaver.rpc.common.enums.RpcResponseCodeEnum;
import com.beaver.rpc.common.exception.RpcException;
import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.domain.RpcResponse;
import com.beaver.rpc.core.invoker.ServiceInvoker;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServiceInvokerImpl implements ServiceInvoker {

    private static final String INTERFACE_NAME = "interfaceName";
    private final ServiceRegistry serviceRegistry;

    private Client transportClient;
    private List<InetSocketAddress> serviceAddressList;

    public ServiceInvokerImpl() {
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }


    @Override
    public void subscribeService(String serviceName) {
        ServiceInvokerImpl instance = this;
        this.serviceAddressList = serviceRegistry.subscribeService(serviceName, new ServiceChangeCallback() {
            @Override
            public void call(List<InetSocketAddress> serviceAddressList) {
                instance.serviceAddressList = serviceAddressList;
            }
        });
    }

    @Override
    public RpcResponse invoke(RpcRequest rpcRequest) throws ExecutionException, InterruptedException, TimeoutException {
        transportClient = new NettyClient(serviceAddressList.get(0));
        RpcResponse<Object> rpcResponse = null;
        if (transportClient instanceof NettyClient) {
            CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) transportClient.sendRequest(rpcRequest);
            long timePassed = System.currentTimeMillis() - rpcRequest.getRequestCreateTime();
            if (timePassed > rpcRequest.getTimeout()) {
                completableFuture.completeExceptionally(new TimeoutException("request "+ rpcRequest.getRequestId()+" time out"));
            }

            rpcResponse = completableFuture.get(rpcRequest.getTimeout() - timePassed, TimeUnit.MILLISECONDS);

        }
        if (transportClient instanceof SocketClient) {
//            CompletableFuture<RpcResponse<Object>> completableFuture = new CompletableFuture<>();
//            completableFuture.complete((RpcResponse<Object>) transportClient.sendRequest(rpcRequest));
//
//            long timePassed = System.currentTimeMillis() - rpcRequest.getRequestCreateTime();
//            if (timePassed > rpcRequest.getTimeout()) {
//                completableFuture.completeExceptionally(new RuntimeException("time out"));
//            } else {
//                rpcResponse = completableFuture.get(rpcRequest.getTimeout() - timePassed, TimeUnit.MILLISECONDS);
//            }

        }
        this.check(rpcResponse, rpcRequest);
        return rpcResponse;
    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
