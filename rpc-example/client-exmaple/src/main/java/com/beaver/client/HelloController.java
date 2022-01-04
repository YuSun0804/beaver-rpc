package com.beaver.client;

import com.beaver.example.api.Hello;
import com.beaver.example.api.HelloService;
import com.beaver.example.api.HelloService2;
import com.beaver.rpc.client.annotation.Reference;
import org.springframework.stereotype.Component;

@Component
public class HelloController {

    @Reference(version = "version1", group = "test1", timeout = 5000)
    private HelloService helloService;

    @Reference(version = "version1", group = "test1", timeout = 5000)
    private HelloService2 helloService2;

    public void test() throws InterruptedException {
        Hello hello = helloService.hello(new Hello("111", "222"));
        String hello2 = helloService2.hello(new Hello("111", "222"));

        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        // assert "Hello description is 222".equals(hello);
        System.out.println(hello);
        System.out.println(hello2);

        Thread.sleep(12000);
        for (int i = 0; i < 10; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
        }
    }
}
