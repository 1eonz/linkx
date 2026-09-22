package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ColumnInfo {
    /**
     * 字段名
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String columnType;

    /**
     * jdbc类型
     */
    private int jdbcType;

    /**
     * 是否非空
     */
    private boolean notNull;

    /**
     * 是否是主键
     */
    private boolean primaryKey;

    /**
     * 字段注释
     */
    private String comment;
}