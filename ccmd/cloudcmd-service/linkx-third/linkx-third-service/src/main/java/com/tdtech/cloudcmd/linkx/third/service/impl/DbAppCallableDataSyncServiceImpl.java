package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.api.dto.KeyValue;
import com.tdtech.cloudcmd.linkx.third.dto.DbPullDataDto;
import com.tdtech.cloudcmd.linkx.third.properties.DbConnectionProperties;
import com.tdtech.cloudcmd.linkx.third.service.AbstractAppCallableDataSyncServiceImpl;
import com.tdtech.cloudcmd.linkx.third.utils.AssertUtils;
import com.tdtech.cloudcmd.linkx.third.utils.KeyValueUtils;
import com.tdtech.cloudcmd.linkx.third.utils.MySqlColumnValidator;
import com.tdtech.cloudcmd.linkx.third.utils.MySqlTableNameValidator;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.DbDialect;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.JdbcTypeMapping;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.MyBatisDynamicConnectionFactory;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * 南向应用数据同步-db类型
 */
@Service("dbAppCallableDataSyncServiceImpl")
@Slf4j
public class DbAppCallableDataSyncServiceImpl extends AbstractAppCallableDataSyncServiceImpl {
    /**
     * 不同数据库类型查询数据sql语句-分页查询
     */
    private static final Map<DbDialect, String> PAGED_SQL_MAP = Map.of(DbDialect.MYSQL,
            "SELECT * from %s  order by %s limit %s,%s ");

    @Autowired
    private MyBatisDynamicConnectionFactory connectionFactory;

    @Autowired
    private IdWorker idWorker;

    @Override
    public AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long page, Long pageSize) throws SQLException {
        // 动态表名存在sql注入风险，需要校验
        AssertUtils.check(MySqlTableNameValidator.isValidTableName(detailVo.getDatabaseName()), "非法的数据库表名");
        // 获取数据库连接
        DbDialect dbDialect = DbDialect.getByCode(detailVo.getDbType());
        AssertUtils.check(Objects.nonNull(dbDialect), "非法的数据库类型");
        DbConnectionProperties connectionProperties = DbConnectionProperties.builder()
                .host(detailVo.getIp())
                .port(String.valueOf(detailVo.getPort()))
                .dialect(dbDialect)
                .dbName(detailVo.getDatabaseName())
                .username(detailVo.getAccount())
                .password(detailVo.getPassword())
                .build();
        // 查询数据,解析列信息
        List<ColumnInfo> columnInfoList = new ArrayList<>();
        DbPullDataDto dbPullDataDto = DbPullDataDto.builder()
                .app(detailVo)
                .connectionProperties(connectionProperties)
                .dbDialect(dbDialect)
                .columnInfoList(columnInfoList)
                .page(page)
                .pageSize(pageSize)
                .build();
        List<JSONObject> resultList = queryData(dbPullDataDto);

        // 未获取到数据信息记录日志
        if (CollectionUtils.isEmpty(columnInfoList) || CollectionUtils.isEmpty(resultList)) {
            log.warn("未解析到数据!");
        }
        return AppDataToDbDto.builder()
                .columnInfoList(columnInfoList)
                .resultDataList(resultList)
                .build();
    }

    private List<JSONObject> queryData(DbPullDataDto dto)
            throws SQLException {
        AppCallableDetailVo app = dto.getApp();
        DbConnectionProperties connectionProperties = dto.getConnectionProperties();
        DbDialect dbDialect = dto.getDbDialect();
        List<ColumnInfo> columnInfoList = dto.getColumnInfoList();
        Long current = dto.getPage();
        Long pageSize = dto.getPageSize();
        // 数据库拉取默认分页
        String sqlTemplate = PAGED_SQL_MAP.get(dbDialect);
        // 校验列名，不能有非法字符
        AssertUtils.check(MySqlColumnValidator.isValidColumnName(app.getUniqueId()), "非法的唯一标识字段名");
        String sql = String.format(sqlTemplate, app.getDataName(), app.getUniqueId(), (current - 1) * pageSize, pageSize);
        List<KeyValue> keyValues = JSON.parseArray(app.getMapper(), KeyValue.class);
        Map<String, String> columnMapper = KeyValueUtils.keyvalueToMap(keyValues);
        List<JSONObject> resultList = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection(connectionProperties)) {
            try (PreparedStatement pstmt = connection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                // 获取字段信息
                getColumnInfo(meta, dbDialect, columnInfoList, columnMapper);
                // 获取数据
                while (rs.next()) {
                    JSONObject json = new JSONObject();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnName(i);
                        Object value = rs.getObject(i);
                        json.put(columnName, value);
                    }
                    // 添加必要参数
                    Date date = new Date();
                    json.put(LINKX_ID_COLUMN, idWorker.nextId());
                    json.put(CREATED_TIME_COLUMN, date);
                    json.put(UPDATE_TIME_COLUMN, date);
                    resultList.add(json);
                }
            } catch (Exception e) {
                log.error("拉取数据失败:", e);
                throw new BusinessException(e);
            }
        }
        return resultList;
    }

    private void getColumnInfo(ResultSetMetaData meta, DbDialect dbDialect,
                               List<ColumnInfo> columnInfoList, Map<String, String> columnMapper)
            throws SQLException {
        int colCount = meta.getColumnCount();
        JdbcTypeMapping jdbcTypeMapping = dbDialect.getMapping();
        for (int i = 1; i <= colCount; i++) {
            int jdbcType = meta.getColumnType(i);
            columnInfoList.add(ColumnInfo.builder()
                    .columnName(meta.getColumnName(i))
                    .columnType(jdbcTypeMapping.getDatabaseType(jdbcType))
                    .jdbcType(jdbcType)
                    .comment(columnMapper.getOrDefault(meta.getColumnName(i), StringUtils.EMPTY))
                    .build());
        }
        addExtraColumn(columnInfoList);
    }
}
