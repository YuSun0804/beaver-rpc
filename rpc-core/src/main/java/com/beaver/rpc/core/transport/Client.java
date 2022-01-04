package com.beaver.rpc.core.transport;

import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.domain.RpcResponse;

import java.util.concurrent.ExecutionException;

public interface Client {
    boolean openClient();

    boolean closeClient();

    Object sendRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException;

    boolean processResponse(RpcResponse rpcResponse);
}
