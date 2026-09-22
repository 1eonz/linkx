package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author :南向应用类型
 */
@AllArgsConstructor
@Getter
public enum AppTypeEnum {

    API(1, "RESTful接口", "apiAppCallableDataSyncServiceImpl",
            "THIRD_APP_DATA_SYNC_API_PAGESIZE", "THIRD_APP_DATA_SYNC_API_PAGE_TIME_INTERVAL"),

    DB(2, "数据库", "dbAppCallableDataSyncServiceImpl",
            "THIRD_APP_DATA_SYNC_DB_PAGESIZE", "THIRD_APP_DATA_SYNC_DB_PAGE_TIME_INTERVAL");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 信息
     */
    private final String msg;

    /**
     * 数据同步的服务实现类
     */
    private final String syncService;

    /**
     * 数据同步时分页大小全局配置中的key
     */
    private final String pageSizeConfigColumn;

    /**
     * 分批次拉取时，时间间隔
     */
    private final String pageTimeIntervalColumn;
}
