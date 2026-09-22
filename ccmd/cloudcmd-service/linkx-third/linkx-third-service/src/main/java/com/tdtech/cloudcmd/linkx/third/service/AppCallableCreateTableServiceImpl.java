package com.tdtech.cloudcmd.linkx.third.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.mapper.AppCallableSyncMapper;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.MysqlDynamicSqlGenerator;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.linkx.third.service.AbstractAppCallableDataSyncServiceImpl.CREATED_TIME_COLUMN;
import static com.tdtech.cloudcmd.linkx.third.service.AbstractAppCallableDataSyncServiceImpl.LINKX_ID_COLUMN;


/**
 * 南向应用创建表相关 服务类
 */
@Service
@Slf4j
public class AppCallableCreateTableServiceImpl implements AppCallableCreateTableService {
    @Autowired
    private AppCallableSyncMapper syncMapper;

    @Autowired
    private IAppCallableService appService;

    @Override
    public void createTable(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto) {
        String tableName = appService.getTableName(appCallable);
        int tableCount = syncMapper.checkTableExists(tableName);
        if (tableCount < 1) {
            String ddl = MysqlDynamicSqlGenerator.generateDDL(tableName, appCallable.getName(),
                    appDataToDbDto.getColumnInfoList(), appCallable.getUniqueId());
            if (!ddl.trim().toUpperCase().startsWith("CREATE TABLE")) {
                throw new BusinessException("仅允许执行DDL创建语句");
            }
            syncMapper.executeUpdateDynamicSql(ddl);
        }
    }

    @Override
    public void createTableAndInsertData(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto) {
        createTable(appCallable, appDataToDbDto);
        doInsertData(appCallable, appDataToDbDto);
    }

    @Override
    public void doInsertData(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto) {
        List<String> unUpdatedColumns = List.of(appCallable.getUniqueId(), LINKX_ID_COLUMN, CREATED_TIME_COLUMN);
        List<String> columns = appDataToDbDto.getColumnInfoList().stream().map(ColumnInfo::getColumnName).collect(Collectors.toList());

        // 分批次写入
        List<List<JSONObject>> partitionList = Lists.partition(appDataToDbDto.getResultDataList(), 500);
        partitionList.forEach(p -> syncMapper.insertOrUpdateAppData(appService.getTableName(appCallable),
                columns, p, unUpdatedColumns));
    }
}
