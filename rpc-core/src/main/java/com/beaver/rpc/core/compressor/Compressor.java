package com.beaver.rpc.core.compressor;

import com.beaver.rpc.common.extension.SPI;

@SPI
public interface Compressor {
    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
