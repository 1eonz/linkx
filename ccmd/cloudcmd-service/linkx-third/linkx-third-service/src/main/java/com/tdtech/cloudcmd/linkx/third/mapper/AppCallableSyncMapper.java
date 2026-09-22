package com.tdtech.cloudcmd.linkx.third.mapper;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableSync;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDataTableColumnInfoVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 南向应用数据同步信息表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Mapper
public interface AppCallableSyncMapper extends BaseMapper<AppCallableSync> {


    /**
     * 执行动态sql，需要拼接好sql-更新数据类，如ddl和update语句
     *
     * @param sql sql语句
     */
    void executeUpdateDynamicSql(@Param("sql") String sql);

    /**
     * 动态插入或更新
     *
     * @param tableName        表名
     * @param columns          有序字段名列表 [id, user_name, age]
     * @param dataList         数据列表
     * @param unUpdatedColumns 不需要更新的列
     */
    int insertOrUpdateAppData(@Param("tableName") String tableName, @Param("columns") List<String> columns,
                              @Param("dataList") List<JSONObject> dataList,
                              @Param("unUpdatedColumns") List<String> unUpdatedColumns
    );

    /**
     * 校验库中是否存在对应表
     *
     * @param tableName 表名
     * @return 对应表存在的数量
     */
    int checkTableExists(@Param("tableName") String tableName);


    /**
     * 获取表字段信息
     *
     * @param tableName 表名
     * @return 表字段信息
     */
    List<AppCallableDataTableColumnInfoVo> getColumnInfo(@Param("tableName") String tableName);
}
