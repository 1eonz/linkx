package com.tdtech.cloudcmd.linkx.third.api.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 南向应用数据入库dto
 */
@Data
@SuperBuilder
public class AppDataToDbDto {

    /**
     * 表字段信息
     */
    private List<ColumnInfo> columnInfoList;

    /**
     * 数据
     */
    private List<JSONObject> resultDataList;

    /**
     * 拉取数据中 dateTimeSign 字段的最早值，供 backward 执行器写入 extendsData.earliestDateTime
     */
    private String minDateTimeSign;

    /**
     * 拉取数据中 dateTimeSign 字段的最晚值，供 forward 执行器写入 extendsData.latestDateTime
     */
    private String maxDateTimeSign;
}