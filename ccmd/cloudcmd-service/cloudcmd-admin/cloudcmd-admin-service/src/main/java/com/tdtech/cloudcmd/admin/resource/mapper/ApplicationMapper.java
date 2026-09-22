package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.Application;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-28
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
    /**
     * 批量插入应用
     * 
     * @param applicationList
     */
    void batchInsertApplication(@Param("applicationList") List<Application> applicationList);

    /**
     * 批量逻辑删除
     * 
     * @param idList
     */
    Integer batchLogicDeleteApplication(@Param("idList") List<Long> idList);
}
