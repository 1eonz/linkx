package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 数据库方言枚举
 * 扩展性：新增数据库只在这里加一行
 */
@Getter
@AllArgsConstructor
public enum DbDialect {

    MYSQL(
            1,
            "com.mysql.jdbc.Driver",
            "jdbc:mysql://%s:%s/%s?%s",
            new MySQLTypeMapping(),
            "THIRD_APP_MYSQL_CONNECT_PARAM",
            "allowPublicKeyRetrieval=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&useSSL=false&zeroDateTimeBehavior=convertToNull&serverTimezone=Asia/Shanghai&autoCommit=true"
    );

    /**
     * 代码
     */
    private final Integer code;

    /**
     * 驱动
     */
    private final String driver;

    /**
     * URL 模板
     */
    private final String urlTemplate;

    /**
     * 类型转换器
     */
    private final JdbcTypeMapping mapping;

    /**
     * url连接参数的全局配置Key
     */
    private final String urlParamConfigKey;

    /**
     * url连接参数的默认值
     */
    private final String urlParamDefault;

    /**
     * 根据code获取数据库
     *
     * @param code code
     * @return 数据库类型
     */
    public static DbDialect getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DbDialect value : DbDialect.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }
        return null;
    }
}