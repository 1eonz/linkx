package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentCoopService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.FunctionalDepartmentCoopMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.FunctionalDepartmentMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门协同岗用户关联服务实现类
 */
@Slf4j
@Service
public class FunctionalDepartmentCoopServiceImpl extends ServiceImpl<FunctionalDepartmentCoopMapper, FunctionalDepartmentCoop>
        implements FunctionalDepartmentCoopService {

    @Resource
    private FunctionalDepartmentMapper functionalDepartmentMapper;

    @Resource
    private FunctionalDepartmentCoopMapper functionalDepartmentCoopMapper;
    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private IdWorker idWorker;

    @Resource
    private ReportUtil reportUtil;

    @Override
    public Page<CoopUser> getCoopUsers(String deptId, Integer pageNum, Integer pageSize,Long orgId) {
        log.info("查询职能部门的协同岗用户，部门ID：{}，页码：{}，每页大小：{}", deptId, pageNum, pageSize);
        
        // 参数校验
        if ("0".equals(deptId)) {
            return new Page<>();
        }

        FunctionalDepartment functionalDepartment = functionalDepartmentMapper.selectInfoById(deptId);
        if (functionalDepartment == null) {
            throw new BusinessException("协同岗层级不存在");
        }
        List<Long> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .collect(Collectors.toList());

        log.info("查询到{}个组织ID用于筛选", orgIds.size());
        // 分页查询
        return functionalDepartmentCoopMapper.getCoopUsers(new Page<>(pageNum, pageSize), deptId, orgIds);
    }

    @Override
    public Page<CoopUser> searchCoopUsers(Integer pageNum, Integer pageSize, Long deptId,
                                                           String name,String startTime, String endTime) {
        return functionalDepartmentCoopMapper.searchCoopUsers(new Page<>(pageNum, pageSize), deptId, name, startTime, endTime);
    }

    @Override
    public Boolean putCoopUsers(Long deptId, List<FunctionalDepartmentCoop> functionalDepartmentCoops) {
        // 参数校验
        if (deptId == null) {
            throw new BusinessException("部门ID不能为空");
        }

        if (deptId == 0L) {
            throw new BusinessException("根节点不能绑定协同岗");
        }

        if (CollectionUtils.isEmpty(functionalDepartmentCoops)) {
            throw new BusinessException("协同岗用户ID列表不能为空");
        }
        FunctionalDepartment functionalDepartment = functionalDepartmentMapper.selectInfoById(String.valueOf(deptId));

        if (functionalDepartment == null) {
            throw new BusinessException("职能部门不存在");
        }
        List<CoopUser> coopUsersList = functionalDepartmentCoopMapper.getCoopUsersList(deptId);


        if (CollectionUtils.isNotEmpty(coopUsersList)) {
            List<Long> existcoopUserIds = coopUsersList.stream().map(CoopUser::getId).collect(Collectors.toList());
            functionalDepartmentCoops = functionalDepartmentCoops
                    .stream().filter(e-> !existcoopUserIds.contains(e.getUserId())).collect(Collectors.toList());
        }

        if(CollectionUtils.isEmpty(functionalDepartmentCoops)){
             throw new BusinessException("所选协同岗都已绑定");
        }

        int memberCount = functionalDepartmentCoopMapper.getMemberCountByLevelId(deptId);

        if (memberCount + functionalDepartmentCoops.size() > 50) {
            throw new BusinessException("协同岗最多绑定50个");
        }

        functionalDepartmentCoops.forEach(functionalDepartmentCoop -> {
            functionalDepartmentCoop.setDeptId(deptId);
            functionalDepartmentCoop.setGmtCreated(new Date());
            functionalDepartmentCoop.setUpdateTime(new Date());
            functionalDepartmentCoop.setIsDeleted(0);
            functionalDepartmentCoop.setId(idWorker.nextId());
        });
        saveBatch(functionalDepartmentCoops);
        // 记录日志
        // 记录修改日志
        saveLog(functionalDepartment);
        return true;
    }

    private void saveLog(FunctionalDepartment functionalDepartment) {
        try {
            OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
            log.setOperation("修改了" + functionalDepartment.getName() + "部门下的协同岗");
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                log.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(log);
        } catch (Exception e) {
            log.error("保存日志失败", e);
        }
    }

    @Override
    public Boolean deleteCoopUsers(List<FunctionalDeleteVO> functionalDeleteVOS) {
        // 参数校验
        if (CollectionUtils.isEmpty(functionalDeleteVOS)) {
            log.warn("ID列表不能为空");
            return false;
        }
        List<Long> ids = functionalDeleteVOS.stream().map(FunctionalDeleteVO::getId).collect(Collectors.toList());
        functionalDepartmentCoopMapper.deleteCoopUsersByids(ids);

        try {
            String departmentName = functionalDeleteVOS.get(0).getDepartmentName();
            String postName = functionalDeleteVOS.stream().map(FunctionalDeleteVO::getPostName).collect(Collectors.joining(","));
            OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
            log.setOperation("删除了" + departmentName + "部门下的" + postName);
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                log.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(log);
        } catch (Exception e) {
            log.error("保存日志失败", e);
        }
        return true;
    }

    @Override
    public Boolean putCoopUsersChecked(List<FunctionalDepartmentCoopCO> functionalDepartmentCoops) {
        if(CollectionUtils.isEmpty(functionalDepartmentCoops)){
            throw new BusinessException("选择的协同岗为空");
        }
        functionalDepartmentCoops.forEach(functionalDepartmentCoop -> {
            UpdateWrapper<FunctionalDepartmentCoop> set = new UpdateWrapper<FunctionalDepartmentCoop>()
                    .eq("id", functionalDepartmentCoop.getId())
                    .set("checked", functionalDepartmentCoop.getChecked())
                    .set("updater", functionalDepartmentCoop.getUpdater());
             update(set);
        });
        try {
            String departmentName = functionalDepartmentCoops.get(0).getDepartmentName();
            String postName = functionalDepartmentCoops.stream().map(FunctionalDepartmentCoopCO::getPostName).collect(Collectors.joining(","));
            OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
            log.setOperation("修改了" + departmentName + "部门下" + postName + "的勾选状态");
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                log.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(log);
        } catch (Exception e) {
            log.error("保存日志失败", e);
        }
        return true;
    }
}