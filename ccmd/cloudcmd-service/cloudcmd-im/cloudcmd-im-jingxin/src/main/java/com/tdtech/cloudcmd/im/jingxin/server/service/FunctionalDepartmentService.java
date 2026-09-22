package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentReq;

import java.util.List;

/**
 * 职能部门服务接口
 */
public interface FunctionalDepartmentService extends IService<FunctionalDepartment> {

    /**
     * 创建职能部门节点
     *
     * @param functionalDepartmentReq 职能部门请求参数
     * @return 是否创建成功
     */
    Boolean createFunctionalDepartmentNode(FunctionalDepartmentReq functionalDepartmentReq);

    /**
     * 修改职能部门节点
     *
     * @param departmentId 部门ID
     * @param functionalDepartmentReq 职能部门请求参数
     * @return 是否修改成功
     */
    Boolean updateFunctionalDepartmentNode(String departmentId, FunctionalDepartmentReq functionalDepartmentReq);

    /**
     * 删除职能部门节点
     *
     * @param departmentId 部门ID
     * @return 是否删除成功
     */
    Boolean deleteFunctionalDepartmentNode(String departmentId);

    /**
     * 查询指定职能部门的子部门
     *
     * @param departmentId 部门ID
     * @return 子部门列表
     */
    List<FunctionalDepartment> childrenDepartmentNode(String departmentId);
}