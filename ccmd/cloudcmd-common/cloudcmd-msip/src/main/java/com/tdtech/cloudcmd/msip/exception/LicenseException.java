package com.tdtech.cloudcmd.msip.exception;

import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务异常.
 *
 */
@Slf4j
public class LicenseException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /**
     * 异常码
     */
    protected int code = 1;

    public LicenseException() {}

    public LicenseException(Throwable cause) {
        super(cause);
    }

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public LicenseException(int code, String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
        this.code = code;
    }

    public LicenseException(ResponseCodeEnum codeEnum, Object... args) {
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
