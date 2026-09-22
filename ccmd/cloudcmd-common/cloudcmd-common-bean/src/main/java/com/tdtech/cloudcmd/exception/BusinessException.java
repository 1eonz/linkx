package com.tdtech.cloudcmd.exception;

import com.tdtech.cloudcmd.enums.ResponseCodeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * 业务异常.
 *
 */
@Slf4j
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 3160241586346324994L;
    /**
     * 异常码
     */
    protected int code = 1;

    public BusinessException() {}

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
        this.code = code;
    }

    public BusinessException(ResponseCodeEnum codeEnum, Object... args) {
        super(String.format(codeEnum.getMsg(), args));
        this.code = codeEnum.getCode();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
