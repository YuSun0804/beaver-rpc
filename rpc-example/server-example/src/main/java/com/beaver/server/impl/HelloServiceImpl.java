package com.beaver.server.impl;

import com.beaver.example.api.Hello;
import com.beaver.example.api.HelloService;
import com.beaver.rpc.client.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Service(group = "test1", version = "version1")
@Component
public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("HelloServiceImpl被创建");
    }

    @Override
    public Hello hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return hello;
    }
}
