package com.tdtech.cloudcmd.im.jingxin.server.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DepartmentLocation;

/**
 * <p>
 * 部门位置信息表 服务类
 * </p>
 *
 * @author example
 * @since 2025-08-13
 */
public interface DepartmentLocationService extends IService<DepartmentLocation> {

    /**
     * 分页查询部门位置信息
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param departmentName 部门名称（可选）
     * @return 分页结果
     */
    IPage<DepartmentLocation> getPageList(Integer pageNum, Integer pageSize, String departmentName);

    /**
     * 保存部门位置信息（新增或编辑）
     * @param departmentLocation 部门位置信息
     * @return 是否成功
     * @throws Exception 验证失败时抛出异常
     */
    boolean saveDepartmentLocation(DepartmentLocation departmentLocation) throws Exception;

    boolean saveLocation(DepartmentLocation departmentLocation) throws Exception;

    boolean updateLocation(DepartmentLocation departmentLocation) throws Exception;

    /**
     * 删除部门位置信息
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteDepartmentLocation(Long id);

    boolean deleteDepartmentLocation(DepartmentLocation departmentLocation);

    DepartmentLocation findByDeptCode(String deptCode);
}