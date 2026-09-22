package com.tdtech.cloudcmd.linkx.third.properties;

import com.tdtech.cloudcmd.linkx.third.utils.datasource.DbDialect;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 数据库连接配置
 */
@Data
@SuperBuilder
public class DbConnectionProperties {
    private DbDialect dialect;
    private String host;
    private String port;
    private String dbName;
    private String username;
    private String password;
}
