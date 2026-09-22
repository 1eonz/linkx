package com.tdtech.cloudcmd.linkx.third.enums;

/**
 * 常量
 */
public interface Constants {

    /**
     * 逻辑删除
     */
    Integer DELETED = 1;

    /**
     * 有效数据
     */
    Integer VALID = 0;

    /**
     * 动态生成表名前缀
     */
    String TABLE_NAME_PREFIX = "tb_app_callable_data_";

    /**
     * 状态信息在事务数据中的字段名
     */
    String LINKX_STATUS_INFO = "linkxStatusInfo";

    /**
     * 逗号
     */
    String COMMA = ",";

    /**
     * 分号
     */
    String SEMICOLON = ";";

}
