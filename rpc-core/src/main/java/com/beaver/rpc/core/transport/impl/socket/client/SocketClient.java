package com.beaver.rpc.core.transport.impl.socket.client;

import com.beaver.rpc.common.domain.RpcResponse;
import com.beaver.rpc.core.invoker.LoadBalancer;
import com.beaver.rpc.core.registry.ServiceRegistry;
import com.beaver.rpc.core.transport.Client;
import com.beaver.rpc.common.exception.RpcException;
import com.beaver.rpc.common.domain.RpcRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class SocketClient implements Client {
    private Socket socket;
    private String serviceName;
    private ServiceRegistry serviceRegistry;
    private List<InetSocketAddress> inetSocketAddressList;
    private LoadBalancer loadBalancer;

    public boolean connectServer() throws IOException {
//        inetSocketAddressList = serviceRegistry.subscribeService(serviceName);
        return false;
    }

    @Override
    public boolean openClient() {
        return false;
    }

    @Override
    public boolean closeClient() {
        return false;
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        try {
            InetSocketAddress inetSocketAddress = loadBalancer.selectService(inetSocketAddressList, rpcRequest);
            socket = new Socket();
            socket.connect(inetSocketAddress);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // Send data to the server through the output stream
            objectOutputStream.writeObject(rpcRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // Read RpcResponse from the input stream
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("调用服务失败:", e);
        }
    }

    @Override
    public boolean processResponse(RpcResponse rpcResponse) {
        return false;
    }
}
