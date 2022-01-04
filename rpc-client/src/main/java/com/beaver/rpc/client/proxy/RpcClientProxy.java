package com.beaver.rpc.client.proxy;

import com.beaver.rpc.common.domain.RpcResponse;
import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.exception.RpcException;
import com.beaver.rpc.core.invoker.ServiceInvoker;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private final RpcService rpcServiceConfig;
    private final ServiceInvoker serviceInvoker;

    public RpcClientProxy(RpcService rpcServiceConfig, ServiceInvoker serviceInvoker) {
        this.rpcServiceConfig = rpcServiceConfig;
        this.serviceInvoker = serviceInvoker;
    }

    /**
     * get the proxy object
     */
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        RpcRequest rpcRequest = RpcRequest.builder().methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .timeout(rpcServiceConfig.getTimeout())
                .requestCreateTime(System.currentTimeMillis())
                .build();
        RpcResponse rpcResponse = null;
        try {
            rpcResponse = serviceInvoker.invoke(rpcRequest);
        } catch (Exception e) {
            throw new RpcException(e);
        }
        return  rpcResponse.getData();
    }
}
