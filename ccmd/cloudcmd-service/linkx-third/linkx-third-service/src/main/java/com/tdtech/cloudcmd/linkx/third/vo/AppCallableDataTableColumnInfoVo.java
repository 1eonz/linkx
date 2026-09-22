package com.tdtech.cloudcmd.linkx.third.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 南向应用-事务数据的字段信息vo
 */
@Data
public class AppCallableDataTableColumnInfoVo {

    /**
     * 字段名称
     */
    private String columnName;


    /**
     * 字段类型
     */
    private String columnType;

    /**
     * 是否配置了映射
     */
    private Integer isMapperColumn = 0;

    /**
     * 映射的字段值
     */
    private String mapperColumnValue;
}
