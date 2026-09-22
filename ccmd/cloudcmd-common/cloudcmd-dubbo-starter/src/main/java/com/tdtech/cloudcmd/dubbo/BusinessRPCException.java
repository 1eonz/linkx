package com.tdtech.cloudcmd.dubbo;

import org.apache.dubbo.rpc.RpcException;

public class BusinessRPCException extends RpcException {
    public BusinessRPCException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessRPCException(String message) {
        super(message);
    }
}
