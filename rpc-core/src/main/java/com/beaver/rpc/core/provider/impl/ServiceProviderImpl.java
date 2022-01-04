package com.beaver.rpc.core.provider.impl;

import com.beaver.rpc.core.provider.ProviderBootStrap;
import com.beaver.rpc.core.registry.ServiceRegistry;
import com.beaver.rpc.common.constant.RpcConstants;
import com.beaver.rpc.common.enums.RpcErrorMessageEnum;
import com.beaver.rpc.common.exception.RpcException;
import com.beaver.rpc.common.extension.ExtensionLoader;
import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.core.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServiceProviderImpl implements ServiceProvider {
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;
    private final ServiceRegistry serviceRegistry;

    public ServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public boolean publishService(RpcService rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, RpcConstants.NETTY_SERVICE_PORT));
            ProviderBootStrap.init();
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
        return true;
    }

    private void addService(RpcService rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

}
