package com.tdtech.cloudcmd.admin.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @author mWX556161
 * @date 2020/11/25 9:34
 */
@Getter
@Setter
public class AdminException extends RuntimeException {

    private int code;

    private Object data;

    public AdminException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AdminException(String message) {
        super(message);
        this.code = -1;
    }

}
