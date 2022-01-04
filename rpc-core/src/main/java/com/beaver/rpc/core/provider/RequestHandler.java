package com.beaver.rpc.core.provider;

import com.beaver.rpc.common.domain.RpcRequest;

public interface RequestHandler {
    Object handle(RpcRequest rpcRequest);
}
