package com.beaver.rpc.core.invoker;

import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.domain.RpcResponse;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ServiceInvoker {
    void subscribeService(String serviceName);

    RpcResponse invoke(RpcRequest rpcRequest) throws ExecutionException, InterruptedException, TimeoutException;
}
