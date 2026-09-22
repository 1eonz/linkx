package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import java.sql.Types;

public abstract class AbstractJdbcTypeMapping implements JdbcTypeMapping {
    /**
     * 公共通用类型，子类可覆盖
     */
    @Override
    public String getDatabaseType(int jdbcType) {
        switch (jdbcType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
                return "VARCHAR(255)";
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
                return "BIGINT";
            case Types.DECIMAL:
            case Types.NUMERIC:
                return "DECIMAL(32,8)";
            case Types.DATE:
                // DATE 类型只存日期，单独映射避免被强转成 DATETIME（多带 00:00:00 时间部分）
                return "DATE";
            case Types.TIMESTAMP:
                return "DATETIME";
            case Types.TIME:
                // TIME 类型表示"时段"，单独映射避免被强转成 DATETIME（带日期）造成语义丢失
                return "TIME";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.FLOAT:
                return "FLOAT";
            default:
                return "TEXT";
        }
    }
}