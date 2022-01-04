package com.beaver.rpc.core.invoker.impl;

import com.beaver.rpc.common.domain.RpcRequest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

public class RandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected InetSocketAddress doSelect(List<InetSocketAddress> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
