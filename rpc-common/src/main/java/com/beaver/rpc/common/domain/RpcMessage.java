package com.beaver.rpc.common.domain;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage<T> {

    //rpc message type
    private byte messageType;
    //serialization type
    private byte serializer;
    //compress type
    private byte compressor;
    //request id
    private int requestId;
    //request/response data
    private T data;

}