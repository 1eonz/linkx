package com.tdtech.cloudcmd.im.jingxin.server.enums;

public interface Constant {
    // 数据保留年限
    int SAVE_YEARS = 3;

    // 加密目录
    String ENCRYPT_DIR = "encrypt";

    /**
     * 逻辑删除
     */
    Integer DELETED = 1;

    /**
     * 有效数据
     */
    Integer VALID = 0;

    Long DEFAULT_DUTY_TYPE_ID = 0L;

    String DEFAULT_DUTY_TYPE_NAME = "默认";

    Long AUTO_DUTY_TYPE_ID = 1L;

    String AUTO_DUTY_TYPE_NAME = "自动上下岗";
}
