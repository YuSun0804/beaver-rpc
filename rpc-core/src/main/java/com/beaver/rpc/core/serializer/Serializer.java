package com.beaver.rpc.core.serializer;

import com.beaver.rpc.common.extension.SPI;

@SPI
public interface Serializer {

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
