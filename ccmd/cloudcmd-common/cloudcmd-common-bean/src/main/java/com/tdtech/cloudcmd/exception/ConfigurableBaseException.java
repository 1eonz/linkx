package com.tdtech.cloudcmd.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ConfigurableBaseException extends AbstractBaseException {
    @Getter
    private final int code;

    @Getter
    private final String msg;

    public ConfigurableBaseException(int code, String msg, Throwable e) {
        super(e);
        this.code = code;
        this.msg = msg;
    }
}
