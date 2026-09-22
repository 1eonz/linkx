package com.tdtech.cloudcmd.linkx.third.utils.datasource;

import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.linkx.third.properties.DbConnectionProperties;
import com.tdtech.cloudcmd.util.StringUtils;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 纯 MyBatis 原生动态连接工具
 * 无 Switch Case
 * 可无限扩展数据库
 * 无连接池
 */
@Component
public class MyBatisDynamicConnectionFactory {


    @Autowired
    private GlobalsRpcService globalsRpcService;

    /**
     * 统一创建连接入口-注意
     *
     * 一套代码适配所有数据库
     */
    public Connection getConnection(DbConnectionProperties properties) throws SQLException {
        // 1. 从方言自动获取驱动 + URL
        DbDialect dialect = properties.getDialect();
        String driver = dialect.getDriver();
        String urlParam = globalsRpcService.getGlobalsValueByName(dialect.getUrlParamConfigKey());
        urlParam = StringUtils.isBlank(urlParam) ? dialect.getUrlParamDefault() : urlParam;
        String url = String.format(dialect.getUrlTemplate(), properties.getHost(), properties.getPort(),
                properties.getDbName(), urlParam);

        // 2. MyBatis 原生无连接池数据源
        UnpooledDataSource dataSource = new UnpooledDataSource();
        dataSource.setDriver(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());

        // 3. MyBatis 原生事务获取连接
        JdbcTransactionFactory factory = new JdbcTransactionFactory();
        JdbcTransaction transaction = (JdbcTransaction) factory.newTransaction(dataSource, null, true);

        return transaction.getConnection();
    }
}