package com.beaver.rpc.core.registry.impl;

import com.beaver.rpc.core.registry.ServiceChangeCallback;
import com.beaver.rpc.core.registry.ServiceRegistry;
import com.beaver.rpc.common.enums.RpcErrorMessageEnum;
import com.beaver.rpc.common.exception.RpcException;
import com.beaver.rpc.common.constant.ZkConstants;
import com.beaver.rpc.common.util.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public boolean registerService(String serviceName, InetSocketAddress serviceAddress) {
        String servicePath = ZkConstants.ZK_REGISTER_ROOT_PATH + "/" + serviceName + serviceAddress.toString();
        CuratorFramework zkClient = ZkUtil.getZkClient();
        ZkUtil.createPersistentNode(zkClient, servicePath);
        log.info("Service " + serviceName + " register success and bind " + serviceAddress);
        return true;
    }

    @Override
    public List<InetSocketAddress> subscribeService(String serviceName, ServiceChangeCallback serviceChangeCallback) {
        CuratorFramework zkClient = ZkUtil.getZkClient();
        List<String> serviceUrlList = ZkUtil.getChildrenNodes(zkClient, serviceName);
        if (serviceUrlList == null || serviceUrlList.size() == 0) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, serviceName);
        }
        List<InetSocketAddress> inetSocketAddressList = ZkUtil.transferService(serviceUrlList);

        ZkUtil.registerWatcher(serviceName, zkClient, serviceChangeCallback);
        return inetSocketAddressList;
    }

   static class ZkUtil {
        private static final int BASE_SLEEP_TIME = 1000;
        private static final int MAX_RETRIES = 3;
        private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
        private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
        private static CuratorFramework zkClient;
        private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
        public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";

        public static void createPersistentNode(CuratorFramework zkClient, String path) {
            try {
                if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                    log.info("The node already exists. The node is:[{}]", path);
                } else {
                    //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                    zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                    log.info("The node was created successfully. The node is:[{}]", path);
                }
                REGISTERED_PATH_SET.add(path);
            } catch (Exception e) {
                log.error("create persistent node for path [{}] fail", path);
            }
        }

        public static CuratorFramework getZkClient() {
            // check if user has set zk address
            Properties properties = PropertiesFileUtil.readPropertiesFile(ZkConstants.RPC_CONFIG_PATH);
            String zookeeperAddress = properties != null && properties.getProperty(ZkConstants.ZK_ADDRESS) != null ? properties.getProperty(ZkConstants.ZK_ADDRESS) : DEFAULT_ZOOKEEPER_ADDRESS;
            // if zkClient has been started, return directly
            if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
                return zkClient;
            }
            // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
            zkClient = CuratorFrameworkFactory.builder()
                    // the server to connect to (can be a server list)
                    .connectString(zookeeperAddress)
                    .retryPolicy(retryPolicy)
                    .build();
            zkClient.start();
            try {
                // wait 30s until connect to the zookeeper
                if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Time out waiting to connect to ZK!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return zkClient;
        }

        /**
         * Gets the children under a node
         *
         * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
         * @return All child nodes under the specified node
         */
        public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
            if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
                return SERVICE_ADDRESS_MAP.get(rpcServiceName);
            }
            List<String> result = null;
            String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
            try {
                result = zkClient.getChildren().forPath(servicePath);
                SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            } catch (Exception e) {
                log.error("get children nodes for path [{}] fail", servicePath);
            }
            return result;
        }

        /**
         * Registers to listen for changes to the specified node
         *
         * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
         */
        private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient, ServiceChangeCallback serviceChangeCallback){
            String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
            PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
                List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
                SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
                List<InetSocketAddress> inetSocketAddressList = transferService(serviceAddresses);
                serviceChangeCallback.call(inetSocketAddressList);
            };
            pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
            try {
                pathChildrenCache.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

       private static  List<InetSocketAddress> transferService(List<String> serviceAddresses) {
           List<InetSocketAddress> inetSocketAddressList = new ArrayList<>();
           for (String serviceUrl: serviceAddresses) {
               String[] socketAddressArray = serviceUrl.split(":");
               String host = socketAddressArray[0];
               int port = Integer.parseInt(socketAddressArray[1]);
               InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
               inetSocketAddressList.add(inetSocketAddress);
           }
           return inetSocketAddressList;
       }

   }
}
