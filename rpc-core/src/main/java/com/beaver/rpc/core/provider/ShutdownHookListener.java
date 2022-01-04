package com.beaver.rpc.core.provider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShutdownHookListener implements Runnable {

    public ShutdownHookListener() {
    }

    @Override
    public void run() {
        log.info("shutdown hook begin......");
        ProviderBootStrap.shutdown();
    }

}