package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import org.springframework.stereotype.Component;

import java.sql.Types;

@Component()
public class MySQLTypeMapping extends AbstractJdbcTypeMapping {
    @Override
    public String getDatabaseType(int jdbcType) {
        switch (jdbcType) {
            case Types.BOOLEAN:
                return "TINYINT(1)";
            case Types.DATE:
                // DATE 类型只存日期，单独映射避免被强转成 DATETIME（多带 00:00:00 时间部分）
                return "DATE";
            case Types.TIMESTAMP:
                return "DATETIME";
            case Types.TIME:
                // TIME 类型表示"时段"，单独映射避免被强转成 DATETIME（带日期）造成语义丢失
                return "TIME";
            default:
                return super.getDatabaseType(jdbcType);
        }
    }
}