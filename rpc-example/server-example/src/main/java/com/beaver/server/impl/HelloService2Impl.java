package com.beaver.server.impl;

import com.beaver.example.api.Hello;
import com.beaver.example.api.HelloService2;
import com.beaver.rpc.client.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Service(group = "test1", version = "version1")
@Component
public class HelloService2Impl implements HelloService2 {

    static {
        System.out.println("HelloServiceImpl2被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello 2 description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
