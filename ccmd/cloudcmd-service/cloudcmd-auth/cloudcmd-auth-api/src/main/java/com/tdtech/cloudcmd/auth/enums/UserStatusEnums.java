package com.tdtech.cloudcmd.auth.enums;

import com.tdtech.cloudcmd.i18n.I18nUtil;

/**
 * @author zhuangzl
 * @date 2020-06-01 19:15
 */
public enum UserStatusEnums {

    USABLE(0, I18nUtil.get("UserStatusEnums_USABLE")), UNUSABLE(1, I18nUtil.get("UserStatusEnums_UNUSABLE")),
    FROZEN(2, I18nUtil.get("UserStatusEnums_FROZEN"));

    private int code;
    private String msg;

    private UserStatusEnums(int code, String msg) {
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
