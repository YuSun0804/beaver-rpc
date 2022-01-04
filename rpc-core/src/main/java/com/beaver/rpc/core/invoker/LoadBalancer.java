package com.beaver.rpc.core.invoker;

import com.beaver.rpc.common.domain.RpcRequest;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {
    InetSocketAddress selectService(List<InetSocketAddress> serviceAddresses, RpcRequest rpcRequest);
}
