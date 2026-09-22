package com.tdtech.cloudcmd.web.advice;

/**
 * 自定义系统异常。主要是抛出系统级的错误。 当httpstatus为404(NOT found),500(内部错误)，504(网关超时),抛出异常
 *
 * @author zWX446107
 */
@SuppressWarnings("serial")
public class SystemException extends RuntimeException {

    private int code;
    private String msg;

    public SystemException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public SystemException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
