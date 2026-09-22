package com.tdtech.cloudcmd.im.jingxin.server.enums;

/**
 * @author ly
 * @date 2025/8/28
 * 切换类型：0手工切换（default），1IM状态变化切换，2值班自动上下岗，3其他
 **/
public enum SwitchTypeEnum {
    USER_CLICK(0, "手工切换"),
    IM_NOTIFY(1, "IM状态变化切换"),
    DUTY_AUTO(2, "值班自动上下岗"),
    ADMIN_CLICK(3, "管理员操作下岗"),
    OTHER(99, "其他");

    private int code;
    private String desc;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    SwitchTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SwitchTypeEnum matchCode(int code) {
        SwitchTypeEnum[] enumArray = SwitchTypeEnum.values();
        for (int i = 0; i < enumArray.length; i++) {
            if (enumArray[i] != null && enumArray[i].getCode() == code) {
                return enumArray[i];
            }
        }
        return null;
    }

}
