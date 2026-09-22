package com.tdtech.cloudcmd.auth.exception;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/25 9:34
 */
@Data
public class OAuthException extends RuntimeException {

    private int code;

    private Object data;

    public OAuthException() {}

    public OAuthException(Throwable cause) {
        super(cause);
    }

    public OAuthException(String message) {
        super(message);
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public OAuthException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

}
