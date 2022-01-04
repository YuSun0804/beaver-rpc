package com.beaver.rpc.core.transport;

import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.common.extension.SPI;

@SPI
public interface Server {

    boolean startServer();

    boolean stopServer();

    boolean addService(RpcService rpcServiceConfig);

    boolean removeService(RpcService rpcServiceConfig);

    boolean processRequest();
}
