package com.beaver.rpc.common.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RpcRequest {
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    private String group;
    private long timeout;
    private long requestCreateTime;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
