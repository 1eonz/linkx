package com.tdtech.cloudcmd.msip.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Data
@Slf4j
/**
 * 存活的告警
 *
 */
public class LiveAlarmsResp {
    /**
     * 告警列表
     */
    private List<AlarmsInfo> content;

    /**
     * 总条数
     */
    private Long totalElements;

    /**
     * 总页数
     */
    private Long totalPages;


    @Data
    public static class AlarmsInfo {
        /**
         * 告警定义信息
         */
        private AlertDefine alertDefine;
        /**
         * 创建时间
         */
        private Date createTime;
    }

    @Data
    public static class AlertDefine {
        /**
         * id
         */
        private String alertDefineId;

        /**
         * 名称
         */
        private String alertName;

        /**
         * 层级
         */
        private String level;
    }
}
