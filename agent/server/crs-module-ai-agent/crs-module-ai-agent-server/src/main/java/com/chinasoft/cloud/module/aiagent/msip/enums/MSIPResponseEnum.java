package com.chinasoft.cloud.module.aiagent.msip.enums;

/**
 * @author ly
 * @date 2025/11/17
 **/
public enum MSIPResponseEnum {
    /**
     * msip相应的code有时候返回数字，有时候返回字符串，乱七八糟的
     */
    COMMON_SUCCESS("0", "上报成功"),

    COMMON_FAIL("PUB-500001", "调用失败"),

    /**
     * 1、上报告警模板时，当模板已存在也是返回-1
     * 2、添加告警，当模板不存在也是返回-1
     * 3、擦除活动告警，当活动告警不存在也是返回-1
     */
    FAILED("-1", "失败");

    /**
     * 告警定义ID
     */
    private String resultCode;

    /**
     * 告警名称
     */
    private String resultInfo;

    MSIPResponseEnum(String resultCode, String resultInfo) {
        this.resultCode = resultCode;
        this.resultInfo = resultInfo;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultInfo() {
        return resultInfo;
    }

    public static MSIPResponseEnum matchCode(String resultCode) {
        MSIPResponseEnum[] values = MSIPResponseEnum.values();
        for (MSIPResponseEnum cur : values) {
            if (cur.getResultCode().equals(resultCode)) {
                return cur;
            }
        }
        return null;
    }
}
