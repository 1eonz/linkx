package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 *
 * 以下说明来自文档：任务模块-250820.pdf
 * Oauth2.0登录，grant type授权枚举如下：
 * 1：authorization_code
 * 2：password
 * 3：client_credentials（仅支持）
 * 4：refresh_token
 * 注意：授权的账号信息需要和任务系统的“system”属性绑定，用于判断账号的授权
 * 范围，才授权账号仅作用于“system”名称范围内的增删改查操作。
 **/
public enum GrantTypeEnum {
    AUTHORIZATION_CODE(1,"authorization_code"),
    PASSWORD(2,"password"),
    CLIENT_CREDENTIALS(3,"client_credentials"),
    REFRESH_TOKEN(4,"refresh_token");

    private Integer code;
    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    GrantTypeEnum(Integer code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public static TasksDeleteTypeEnum matchCode(int code) {
        TasksDeleteTypeEnum[] enumArray = TasksDeleteTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
