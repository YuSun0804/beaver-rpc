/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.beaver.rpc.core.provider;

import com.beaver.rpc.common.extension.ExtensionLoader;
import com.beaver.rpc.core.transport.Server;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class ProviderBootStrap {

    private static volatile boolean isInitialized = false;
    static volatile Map<String, Server> serversMap = new HashMap<String, Server>();

    public static void init() {
        if (!isInitialized) {
            synchronized (ProviderBootStrap.class) {
                if (!isInitialized) {
                    Server server = ExtensionLoader.getExtensionLoader(Server.class).getExtension("netty");
                    server.startServer();
                    isInitialized = true;
                    serversMap.put("netty", server);
                }
            }
        }
    }

    public static void shutdown() {
        for (Server server : serversMap.values()) {
            if (server != null) {
                log.info("start to stop " + server);
                server.stopServer();
            }
        }
    }

}
