package com.beaver.rpc.core.transport.impl.socket.server;

import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.core.transport.Server;
import com.beaver.rpc.common.domain.RpcRequest;
import com.beaver.rpc.common.domain.RpcResponse;
import com.beaver.rpc.core.provider.impl.RequestHandlerImpl;
import com.beaver.rpc.common.util.ThreadPoolFactoryUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

@Slf4j
public class SocketServer implements Server {

    private ExecutorService threadPool;
    private RequestHandlerImpl rpcRequestHandler;
    private final static int PORT = 8080;

    public SocketServer() {
        threadPool = ThreadPoolFactoryUtils.createCustomThreadPoolIfAbsent("socket-server-rpc-pool");
    }

    @Override
    public boolean startServer() {
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, PORT));
            // CustomShutdownHook.getCustomShutdownHook().clearAll();
            while (true) {
                Socket socket = server.accept();
                log.info("client connected [{}]", socket.getInetAddress());
                threadPool.execute(()-> doProcess(socket));
            }
            //threadPool.shutdown();
        } catch (IOException e) {
            log.error("occur IOException:", e);
        }
        return false;
    }

    @Override
    public boolean stopServer() {
        return false;
    }

    @Override
    public boolean addService(RpcService rpcServiceConfig) {
        return false;
    }

    @Override
    public boolean removeService(RpcService rpcServiceConfig) {
        return false;
    }

    @Override
    public boolean processRequest() {
        return false;
    }

    private void doProcess(Socket socket) {
        log.info("server handle message from client by thread: [{}]", Thread.currentThread().getName());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            Object result = rpcRequestHandler.handle(rpcRequest);
            objectOutputStream.writeObject(RpcResponse.success(result, rpcRequest.getRequestId()));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error("occur exception:", e);
        }
    }
}
