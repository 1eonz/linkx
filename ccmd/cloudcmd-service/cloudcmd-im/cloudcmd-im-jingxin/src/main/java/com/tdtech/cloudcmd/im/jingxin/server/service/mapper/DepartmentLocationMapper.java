package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DepartmentLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 部门位置信息表 Mapper 接口
 * </p>
 ** @since 2025-08-13
 */
@Mapper
public interface DepartmentLocationMapper extends BaseMapper<DepartmentLocation> {

    /**
     * 分页查询部门位置信息，支持按部门名称搜索
     * @param page 分页参数
     * @param departmentName 部门名称（模糊搜索）
     * @return 分页结果
     */
    IPage<DepartmentLocation> selectPageByDepartmentName(Page<DepartmentLocation> page, @Param("departmentName") String departmentName,@Param("orgIds") List<String> orgIds);

    /**
     * 根据部门编码查询记录数
     * @param departmentId 部门编码
     * @param excludeId 排除的ID（用于编辑时校验）
     * @return 记录数
     */
    int countByDepartmentId(@Param("departmentCode") String departmentCode, @Param("excludeId") Long excludeId);
}

