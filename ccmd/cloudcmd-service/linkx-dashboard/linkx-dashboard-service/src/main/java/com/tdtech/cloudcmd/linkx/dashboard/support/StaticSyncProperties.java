package com.tdtech.cloudcmd.linkx.dashboard.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统计同步定时任务配置。
 * <p>
 * 默认每小时全量执行一轮，每批拉 500 条，可按表覆盖。
 * 通过 application.yml / Nacos 配置 {@code dashboard.static-sync.*} 调整。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dashboard.static-sync")
public class StaticSyncProperties {

    /**
     * 是否启用统计同步定时任务（总开关，默认开）。
     */
    private boolean enabled = true;

    /**
     * 每批拉取大小，默认 500。
     */
    private int pageSize = 500;

    /**
     * 单轮拉取的最大批数（防源表数据量异常导致长时间阻塞），默认 200 批 = 10 万条。
     */
    private int maxBatchesPerRound = 200;

    /**
     * cron 表达式，默认每小时整点执行。
     */
    private String cron = "0 0 * * * ?";
}