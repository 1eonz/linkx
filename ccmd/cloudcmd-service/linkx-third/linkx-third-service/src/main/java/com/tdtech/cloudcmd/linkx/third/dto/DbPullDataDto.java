package com.tdtech.cloudcmd.linkx.third.dto;

import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.properties.DbConnectionProperties;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.DbDialect;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class DbPullDataDto {
    /**
     * 南向应用信息
     */
    private AppCallableDetailVo app;

    /**
     * 数据库连接信息
     */
    private DbConnectionProperties connectionProperties;

    /**
     * 数据库方言
     */
    private DbDialect dbDialect;

    /**
     * 数据库列信息
     */
    private List<ColumnInfo> columnInfoList;

    /**
     * 页码
     */
    private Long page;

    /**
     * 页面大小
     */
    private Long pageSize;
}