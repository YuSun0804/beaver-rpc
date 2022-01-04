package com.beaver.rpc.core.registry;

import java.net.InetSocketAddress;
import java.util.List;

public interface ServiceChangeCallback {
    void call(List<InetSocketAddress> serviceAddressList);
}
