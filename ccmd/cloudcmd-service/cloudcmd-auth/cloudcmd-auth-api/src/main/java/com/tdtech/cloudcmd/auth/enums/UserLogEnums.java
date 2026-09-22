package com.tdtech.cloudcmd.auth.enums;

import com.tdtech.cloudcmd.i18n.I18nUtil;

/**
 * @author zhuangzl
 * @date 2020-06-02 19:15
 */
public enum UserLogEnums {

    UNKNOWN(0, I18nUtil.get("UserLogEnums_UNKNOWN")), LOGIN(1, "UserLogEnums_LOGIN"), LOGOUT(2, "UserLogEnums_LOGOUT"),
    LOCKED(3, I18nUtil.get("UserLogEnums_LOCKED")), FROZEN(4, I18nUtil.get("UserLogEnums_USER_FROZEN")),
    DUPLICATE(5, "UserLogEnums_DUPLICATE"), UPDATAPWD(6, "UserLogEnums_UPDATAPWD"),
    FAIL(7,"UserLogEnums_LOGIN_FAILED");

    private int code;
    private String msg;

    private UserLogEnums(int code, String msg) {
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
