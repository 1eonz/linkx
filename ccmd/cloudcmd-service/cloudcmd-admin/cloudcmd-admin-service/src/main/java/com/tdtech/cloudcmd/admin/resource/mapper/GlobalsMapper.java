package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;

/**
 * <p>
 * 全局变量信息表 Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Mapper
public interface GlobalsMapper extends BaseMapper<Globals> {
    /**
     * 批量插入扩展配置
     * 
     * @param globalsList
     */
    void batchInsertGlobals(@Param("globalsList") List<Globals> globalsList);

    /**
     * 根据id逻辑删除全局配置
     * 
     * @param id
     */
    void batchLogicDeleteGlobalsById(@Param("id") Long id);
}
