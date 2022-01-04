package com.beaver.rpc.core.invoker.impl;

import com.beaver.rpc.core.invoker.LoadBalancer;
import com.beaver.rpc.common.domain.RpcRequest;

import java.net.InetSocketAddress;
import java.util.List;

public abstract class AbstractLoadBalancer implements LoadBalancer {

    @Override
    public InetSocketAddress selectService(List<InetSocketAddress> serviceAddresses, RpcRequest rpcRequest) {
        if (serviceAddresses == null || serviceAddresses.size() == 0) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract InetSocketAddress doSelect(List<InetSocketAddress> serviceAddresses, RpcRequest rpcRequest);

}
