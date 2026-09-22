package com.tdtech.cloudcmd.linkx.third.mapper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.linkx.third.api.dto.SearchPair;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 南向应用信息表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Mapper
public interface AppCallableMapper extends BaseMapper<AppCallable> {

    /**
     * 查询南向应用数据的分页
     *
     * @param tableName 表名
     * @param current   页码
     * @param pageSize  页面尺寸
     * @param searchPair 搜索参数
     * @param timeColumn 时间列名, 用于排序
     * @return 分页数据
     */
    List<JSONObject> pageAppData(@Param("tableName")String tableName, @Param("current") Long current,
                                 @Param("pageSize") Long pageSize, @Param("searchPair") SearchPair<String, String> searchPair,
                                 @Param("timeColumn") String timeColumn);

    /**
     *  查询南向应用数据的总条数
     *
     * @param tableName 表名
     * @return 数据量
     */
    Long countAppData(@Param("tableName") String tableName, @Param("searchPair") SearchPair<String, String> searchPair);

    /**
     * 通过id查询南向应用事务信息
     * @param tableName 表名
     * @param linkxId 表中id
     */
    JSONObject getAppDataById(@Param("tableName") String tableName, @Param("linkxId") Long linkxId);
}
