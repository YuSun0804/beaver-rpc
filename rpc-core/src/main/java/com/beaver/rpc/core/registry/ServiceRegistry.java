package com.beaver.rpc.core.registry;

import com.beaver.rpc.common.extension.SPI;

import java.net.InetSocketAddress;
import java.util.List;

@SPI
public interface ServiceRegistry {
    /**
     * used this method to register a service, start registry in this method if necessary
     *
     * @param serviceName
     * @param serviceAddress
     * @return
     */
    boolean registerService(String serviceName, InetSocketAddress serviceAddress);

    /**
     * used this method to subscribe a service, watch the service changing and notify the invoker
     *
     * @param serviceName
     * @param serviceChangeCallback
     * @return
     */
    List<InetSocketAddress> subscribeService(String serviceName, ServiceChangeCallback serviceChangeCallback);
}
