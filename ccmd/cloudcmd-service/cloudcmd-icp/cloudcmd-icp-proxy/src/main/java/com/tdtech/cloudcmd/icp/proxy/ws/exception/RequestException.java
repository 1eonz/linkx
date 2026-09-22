package com.tdtech.cloudcmd.icp.proxy.ws.exception;

public class RequestException extends RuntimeException {

    public RequestException(String msg) {
        super(msg);
    }

    public RequestException(String msg, Throwable e) {
        super(msg, e);
    }
}