package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefault;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefaultReq;

import java.util.List;

/**
 * 部门协同岗用户关联默认表服务接口
 */
public interface FunctionalDepartmentCoopDefaultService extends IService<FunctionalDepartmentCoopDefault> {

    /**
     * 分页查询默认协同岗用户
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<FunctionalDepartmentCoopDefault> page(Long pageNum, Long pageSize);

    /**
     * 查询所有默认协同岗用户
     *
     * @return 默认协同岗用户列表
     */
    List<FunctionalDepartmentCoopDefault> listAll();

    /**
     * 创建默认协同岗用户
     *
     * @param req 请求参数
     * @return 是否创建成功
     */
    Boolean create(FunctionalDepartmentCoopDefaultReq req);

    /**
     * 批量创建默认协同岗用户
     *
     * @param req 请求参数
     * @return 是否创建成功
     */
    Boolean batchCreate(FunctionalDepartmentCoopDefaultReq req);

    /**
     * 更新默认协同岗用户
     *
     * @param id  主键ID
     * @param req 请求参数
     * @return 是否更新成功
     */
    Boolean update(String id, FunctionalDepartmentCoopDefaultReq req);

    /**
     * 删除默认协同岗用户
     *
     * @param id 主键ID
     * @return 是否删除成功
     */
    Boolean delete(Long id);

    /**
     * 批量删除默认协同岗用户
     *
     * @param ids 主键ID列表
     * @return 是否删除成功
     */
    Boolean batchDelete(List<Long> ids);

    Page<CoopUser> listPage(Integer pageNum, Integer pageSize, String name,Long orgId);
}