package com.tdtech.cloudcmd.linkx.third.dto;

import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
public class ApiPullDataDto {
    /**
     * 南向应用信息
     */
    private AppCallableDetailVo app;

    /**
     * 数据库列信息
     */
    private List<ColumnInfo> columnInfoList;

    /**
     * 页码
     */
    private Long page;

    /**
     * 页面大小
     */
    private Long pageSize;

    /**
     * 时间范围起始（用于替换请求参数中的时间表达式）
     */
    private LocalDateTime timeRangeStart;

    /**
     * 时间范围结束（用于替换请求参数中的时间表达式）
     */
    private LocalDateTime timeRangeEnd;

    /**
     * 拉取数据中 dateTimeSign 字段的最早值（由 refactorDataList 在遍历中填充）
     */
    private String minDateTimeSign;

    /**
     * 拉取数据中 dateTimeSign 字段的最晚值（由 refactorDataList 在遍历中填充）
     */
    private String maxDateTimeSign;
}