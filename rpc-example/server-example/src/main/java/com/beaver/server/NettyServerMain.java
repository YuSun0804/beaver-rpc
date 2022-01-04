package com.beaver.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NettyServerMain {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("applicationContext.xml");
    }
}
