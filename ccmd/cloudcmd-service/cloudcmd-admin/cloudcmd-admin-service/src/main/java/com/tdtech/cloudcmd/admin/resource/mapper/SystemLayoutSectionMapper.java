package com.tdtech.cloudcmd.admin.resource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.SystemLayoutSection;

/**
 * 系统布局板块 Mapper 接口
 *
 * @author: S063874
 * @date: 2026-03-10 14:08
 */
@Mapper
public interface SystemLayoutSectionMapper extends BaseMapper<SystemLayoutSection> {

    /**
     * 根据类型查询是否存在该类型的板块（未删除）
     *
     * @param type 板块类型
     * @return 存在返回 true，不存在返回 false
     */
    @Select("SELECT COUNT(*) > 0 FROM tb_system_layout_section WHERE `type` = #{type} AND deleted = 0")
    boolean existsByType(@Param("type") Integer type);

    @Select("SELECT COUNT(*) > 0 FROM tb_system_layout_section WHERE `type` = #{type} AND deleted = 0  AND id != #{id}")
    boolean existsByTypeAndId(@Param("type") Integer type,@Param("id") Long id);

    /**
     * 根据类型查询该类型的板块数量（未删除）
     *
     * @param type 板块类型
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_system_layout_section WHERE `type` = #{type} AND deleted = 0")
    int countByType(@Param("type") Integer type);
}