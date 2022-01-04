package com.beaver.rpc.common.util;

public class RuntimeUtil {
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
