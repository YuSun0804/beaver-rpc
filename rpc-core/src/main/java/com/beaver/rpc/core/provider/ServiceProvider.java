package com.beaver.rpc.core.provider;

import com.beaver.rpc.common.domain.RpcService;

public interface ServiceProvider {
    Object getService(String rpcServiceName);

    boolean publishService(RpcService rpcServiceConfig);
}
