package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDeleteVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoop;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopCO;

import java.util.List;

/**
 * 部门协同岗用户关联服务接口
 */
public interface FunctionalDepartmentCoopService extends IService<FunctionalDepartmentCoop> {

    /**
     * 查询指定职能部门的协同岗用户（分页）
     *
     * @param deptId   职能部门ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<CoopUser> getCoopUsers(String deptId, Integer pageNum, Integer pageSize,Long orgId);

    /**
     * 查询协同岗用户（带搜索条件，分页）
     *
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @param deptId     职能部门ID

     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 分页结果
     */
    Page<CoopUser> searchCoopUsers(Integer pageNum, Integer pageSize, Long deptId,
                                                    String name,String startTime, String endTime);


    /**
     * 修改指定职能部门的协同岗用户
     *
     * @param deptId      职能部门ID
     * @param functionalDepartmentCoops 协同岗用户ID列表
     * @return 是否修改成功
     */
    Boolean putCoopUsers(Long deptId , List<FunctionalDepartmentCoop> functionalDepartmentCoops);

    /**
     * 删除协同岗用户关联
     *
     * @param ids 关联ID列表
     * @return 是否删除成功
     */
    Boolean deleteCoopUsers(List<FunctionalDeleteVO> functionalDeleteVOS);

    Boolean putCoopUsersChecked(List<FunctionalDepartmentCoopCO> functionalDepartmentCoops);
}