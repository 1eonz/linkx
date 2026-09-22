package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职能部门 Mapper 接口
 */
@Mapper
public interface FunctionalDepartmentMapper extends BaseMapper<FunctionalDepartment> {

    /**
     * 根据部门名称统计数量
     *
     * @param name 部门名称
     * @return 数量
     */
    int selectByNameCount(@Param("name") String name);

    /**
     * 根据父部门ID查询部门信息
     *
     * @param parentId 父部门ID
     * @return 部门信息
     */
    FunctionalDepartment selectByParentId(@Param("parentId") Long parentId);

    /**
     * 统计子部门数量
     *
     * @param parentId 父部门ID
     * @return 子部门数量
     */
    int selectChildrenCount(@Param("parentId") Long parentId);

    /**
     * 查询子部门列表
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    List<FunctionalDepartment> selectChildren(@Param("parentId") Long parentId);

    /**
     * 根据部门ID查询部门详细信息
     *
     * @param id 部门ID
     * @return 部门信息
     */
    FunctionalDepartment selectInfoById(@Param("id") String id);

    /**
     * 批量查询所有子部门数量
     *
     * @param ids 部门ID列表
     * @return 部门列表（包含子部门数量信息）
     */
    List<FunctionalDepartment> selectAllChildrenCount(@Param("ids") List<Long> ids);
}