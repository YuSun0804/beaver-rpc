package com.beaver.rpc.client.spring;

import com.beaver.rpc.client.annotation.Reference;
import com.beaver.rpc.client.annotation.Service;
import com.beaver.rpc.client.proxy.RpcClientProxy;
import com.beaver.rpc.common.domain.RpcService;
import com.beaver.rpc.common.util.SingletonFactory;
import com.beaver.rpc.core.invoker.ServiceInvoker;
import com.beaver.rpc.core.invoker.impl.ServiceInvokerImpl;
import com.beaver.rpc.core.provider.ServiceProvider;
import com.beaver.rpc.core.provider.impl.ServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final ServiceInvoker serviceInvoker;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
        this.serviceInvoker = SingletonFactory.getInstance(ServiceInvokerImpl.class);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(Service.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), Service.class.getCanonicalName());
            // get RpcService annotation
            Service rpcService = bean.getClass().getAnnotation(Service.class);
            // build RpcServiceProperties
            RpcService rpcServiceConfig = RpcService.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Reference rpcReference = declaredField.getAnnotation(Reference.class);
            if (rpcReference != null) {
                RpcService rpcServiceConfig = RpcService.builder()
                        .group(rpcReference.group())
                        .timeout(rpcReference.timeout())
                        .version(rpcReference.version()).build();
                String serviceName = declaredField.getType().getCanonicalName() + rpcServiceConfig.getGroup() + rpcServiceConfig.getVersion();
                serviceInvoker.subscribeService(serviceName);
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcServiceConfig, serviceInvoker);
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
