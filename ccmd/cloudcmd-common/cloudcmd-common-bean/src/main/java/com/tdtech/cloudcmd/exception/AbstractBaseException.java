package com.tdtech.cloudcmd.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class AbstractBaseException extends RuntimeException {

    public AbstractBaseException(Throwable e) {
        super(e);
    }

    public abstract int getCode();

    public abstract String getMsg();

}
