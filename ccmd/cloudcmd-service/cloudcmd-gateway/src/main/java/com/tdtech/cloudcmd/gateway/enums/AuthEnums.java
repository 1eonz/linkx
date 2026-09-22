package com.tdtech.cloudcmd.gateway.enums;

/**
 * @author zhuangzl
 * @date 2020-08-08 13:02
 */
public enum AuthEnums {

    AUTH_TOKEN_NOT_FIND(401, "缺失令牌,鉴权失败"), AUTH_TOKEN_INVALID(401, "认证不通过,用户尚未登录"),
    AUTH_TOKEN_TIMEOUT(401, "token已超时，请重新登录"), NO_HEADER_APPKEY(401, "请求缺失header--'x-cloudcmd-appkey'"),
    TOKEN_ILLEGAL(401, "非该页面token"),AUTH_PERMISSION_AUTHENTICATION_FAILED(401, "权限认证失败"),
    AUTH_TOKEN_USER_PER(402, "用户已过期，请联系管理员");

    private int code;
    private String msg;

    private AuthEnums(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
