package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ChinasoftPortal
 * @date 2025/9/12
 * @Describe：
 */

@Data
public class ArchiveTimelineDTO {
    /**
     * 年月组合（格式：yyyy-MM，如2025-09）
     */
    private String yearMonth;
    /**
     * 当月归档数量
     */
    private Long count;
}
