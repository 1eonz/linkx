package com.tdtech.cloudcmd.msip.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class QueryLiveAlarmsReq {
    /**
     * 页码
     */
    private Long pageNum;

    /**
     * 页面尺寸
     */
    private Long pageSize;

    /**
     * 语言，默认中文zh
     */
    private String language = "zh";
}
