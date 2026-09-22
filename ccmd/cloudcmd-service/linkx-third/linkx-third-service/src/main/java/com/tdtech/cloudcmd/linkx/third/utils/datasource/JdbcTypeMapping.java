package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import java.sql.Types;

/**
 * 数据库字段类型转换器
 */
public interface JdbcTypeMapping {
    // 根据 JDBC Type 返回对应数据库的字段类型
    String getDatabaseType(int jdbcType);
}