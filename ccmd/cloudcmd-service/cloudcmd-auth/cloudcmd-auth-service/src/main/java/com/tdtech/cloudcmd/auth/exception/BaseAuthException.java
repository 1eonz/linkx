package com.tdtech.cloudcmd.auth.exception;

import lombok.Data;

/**
 * @author zhuangzl
 * @date 2020-06-01 15:36
 */
@Data
public class BaseAuthException extends RuntimeException {

    private int code;

    public BaseAuthException() {}

    public BaseAuthException(Throwable cause) {
        super(cause);
    }

    public BaseAuthException(String message) {
        super(message);
    }

    public BaseAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseAuthException(int code, String message) {
        super(message);
        this.code = code;
    }
}
