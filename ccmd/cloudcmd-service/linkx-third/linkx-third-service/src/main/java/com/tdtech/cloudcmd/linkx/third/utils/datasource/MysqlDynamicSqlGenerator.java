package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * mysql动态sql生成器
 */
public class MysqlDynamicSqlGenerator {

    /**
     * 动态生成ddl
     *
     * @param tableName      表名
     * @param tableComment   表注释
     * @param columnInfoList 列信息
     * @param uniqueColumn   唯一索引列名
     * @return sql
     */
    public static String generateDDL(String tableName, String tableComment,
                                     List<ColumnInfo> columnInfoList, String uniqueColumn) {
        if (StringUtils.isBlank(tableName) || CollectionUtils.isEmpty(columnInfoList) || StringUtils.isBlank(uniqueColumn)) {
            return StringUtils.EMPTY;
        }
        String columnsSql = columnInfoList.stream()
                .map(col -> {
                    StringBuilder sb = new StringBuilder();
                    // 字段类型
                    sb.append("`").append(col.getColumnName()).append("` ").append(col.getColumnType());
                    if (col.isNotNull()) {
                        sb.append(" NOT NULL");
                    }

                    // 是否主键
                    if (col.isPrimaryKey()) {
                        sb.append(" PRIMARY KEY");
                    }

                    // 注释
                    if (col.getComment() != null && !col.getComment().isEmpty()) {
                        sb.append(" COMMENT '").append(col.getComment()).append("'");
                    }
                    return sb.toString();
                }).collect(Collectors.joining(", "));
        // 拼接索引列
        columnsSql = columnsSql + ", UNIQUE INDEX uk_" + uniqueColumn + "(" + uniqueColumn + ")";
        return String.format("CREATE TABLE IF NOT EXISTS `%s` (%s) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci " +
                "COMMENT='%s'", tableName, columnsSql, tableComment);
    }
}
