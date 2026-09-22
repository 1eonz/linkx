package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefault;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefaultReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentCoopDefaultService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.FunctionalDepartmentCoopDefaultMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门协同岗用户关联默认表服务实现类
 */
@Slf4j
@Service
public class FunctionalDepartmentCoopDefaultServiceImpl extends ServiceImpl<FunctionalDepartmentCoopDefaultMapper, FunctionalDepartmentCoopDefault>
        implements FunctionalDepartmentCoopDefaultService {


    @Resource
    private IdWorker idWorker;

    @Resource
    private FunctionalDepartmentCoopDefaultMapper functionalDepartmentCoopDefaultMapper;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private ReportUtil reportUtil;

    @Override
    public Page<FunctionalDepartmentCoopDefault> page(Long pageNum, Long pageSize) {
        log.info("分页查询默认协同岗用户，页码：{}，每页大小：{}", pageNum, pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<FunctionalDepartmentCoopDefault> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FunctionalDepartmentCoopDefault::getGmtCreated);
        
        // 分页查询
        Page<FunctionalDepartmentCoopDefault> page = new Page<>(pageNum, pageSize);
        Page<FunctionalDepartmentCoopDefault> result = page(page, queryWrapper);
        
        log.info("查询到默认协同岗用户数量：{}", result.getTotal());
        return result;
    }

    @Override
    public List<FunctionalDepartmentCoopDefault> listAll() {
        log.info("查询所有默认协同岗用户");
        
        // 构建查询条件
        LambdaQueryWrapper<FunctionalDepartmentCoopDefault> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FunctionalDepartmentCoopDefault::getGmtCreated);
        queryWrapper.eq(FunctionalDepartmentCoopDefault::getIsDeleted, 0);
        List<FunctionalDepartmentCoopDefault> list = list(queryWrapper);
        
        log.info("查询到默认协同岗用户数量：{}", list.size());
        return list;
    }

    @Override
    public Boolean create(FunctionalDepartmentCoopDefaultReq req) {
        log.info("创建默认协同岗用户，参数：{}", req);
        
        // 参数校验
        if (req.getUserId() == null) {
            log.warn("协同岗用户ID不能为空");
            return false;
        }

        // 检查是否已存在
        LambdaQueryWrapper<FunctionalDepartmentCoopDefault> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionalDepartmentCoopDefault::getUserId, req.getUserId());
        long count = count(queryWrapper);
        if (count > 0) {
            log.warn("协同岗用户已存在，用户ID：{}", req.getUserId());
            return false;
        }

        // 创建实体
        FunctionalDepartmentCoopDefault entity = new FunctionalDepartmentCoopDefault();
        entity.setUserId(req.getUserId());
        entity.setId(idWorker.nextId());
        entity.setCreator(req.getCreator());
        entity.setIsDeleted(0);
        entity.setGmtCreated(new Date());
        
        // 保存
        boolean result = save(entity);
        
        log.info("创建默认协同岗用户完成，结果：{}", result);
        return result;
    }

    @Override
    public Boolean batchCreate(FunctionalDepartmentCoopDefaultReq req) {
        log.info("批量创建默认协同岗用户，参数：{}", req);
        
        // 参数校验
        if (CollectionUtils.isEmpty(req.getUserIds())) {
            log.warn("协同岗用户ID列表不能为空");
            return false;
        }

        try {
            // 查询已存在的用户ID
            LambdaQueryWrapper<FunctionalDepartmentCoopDefault> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(FunctionalDepartmentCoopDefault::getUserId, req.getUserIds());
            List<FunctionalDepartmentCoopDefault> existingList = list(queryWrapper);
            List<Long> existingUserIds = existingList.stream()
                    .map(FunctionalDepartmentCoopDefault::getUserId)
                    .collect(Collectors.toList());
            
            // 过滤掉已存在的用户ID
            List<Long> newUserIds = req.getUserIds().stream()
                    .filter(userId -> !existingUserIds.contains(userId))
                    .collect(Collectors.toList());
            
            if (CollectionUtils.isEmpty(newUserIds)) {
                log.warn("所有协同岗用户都已存在");
                return true;
            }
            
            // 批量创建实体
            List<FunctionalDepartmentCoopDefault> entityList = new ArrayList<>();
            for (Long userId : newUserIds) {
                FunctionalDepartmentCoopDefault entity = new FunctionalDepartmentCoopDefault();
                entity.setUserId(userId);
                entity.setCreator(req.getCreator());
                entity.setIsDeleted(0);
                entity.setGmtCreated(new Date());
                entityList.add(entity);
            }
            
            // 批量保存
            boolean result = saveBatch(entityList);

            try {
                OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
                log.setOperation("设置了职能分类默认协同岗");
                UserInfo user = SecurityUtils.getUser();
                if (Objects.nonNull(user)) {
                    log.setOperator(user.getUserName());
                }
                reportUtil.saveOperationLog(log);
            } catch (Exception e) {
                log.error("保存日志失败", e);
            }
            return result;
        } catch (Exception e) {
            log.error("批量创建默认协同岗用户失败", e);
            return false;
        }
    }

    @Override
    public Boolean update(String id, FunctionalDepartmentCoopDefaultReq req) {
        log.info("更新默认协同岗用户，ID：{}，参数：{}", id, req);
        
        // 参数校验
        if (StringUtils.isBlank(id)) {
            log.warn("主键ID不能为空");
            return false;
        }

        if (req.getUserId() == null) {
            log.warn("协同岗用户ID不能为空");
            return false;
        }

        // 查询是否存在
        FunctionalDepartmentCoopDefault existing = getById(id);
        if (existing == null) {
            log.warn("默认协同岗用户不存在，ID：{}", id);
            return false;
        }

        // 更新实体
        FunctionalDepartmentCoopDefault entity = new FunctionalDepartmentCoopDefault();
        entity.setId(Long.parseLong(id));
        entity.setUserId(req.getUserId());
        entity.setCreator(req.getCreator());
        
        // 更新
        boolean result = updateById(entity);
        
        log.info("更新默认协同岗用户完成，结果：{}", result);
        return result;
    }

    @Override
    public Boolean delete(Long id) {
        UpdateWrapper<FunctionalDepartmentCoopDefault> in = new UpdateWrapper<FunctionalDepartmentCoopDefault>()
                .set("is_deleted", 1)
                .eq("id", id);
        boolean update = update(in);
        try {
            OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
            log.setOperation("删除了职能分类默认协同岗");
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                log.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(log);
        } catch (Exception e) {
            log.error("保存日志失败", e);
        }

        return update;
    }

    @Override
    public Boolean batchDelete(List<Long> ids) {
        log.info("批量删除默认协同岗用户，ID列表：{}", ids);

        UpdateWrapper<FunctionalDepartmentCoopDefault> in = new UpdateWrapper<FunctionalDepartmentCoopDefault>()
                .set("is_deleted", 1)
                .in("id", ids);
        return update(in);
    }

    @Override
    public Page<CoopUser> listPage(Integer pageNum, Integer pageSize, String name,Long orgId) {
        List<Long> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .collect(Collectors.toList());
        return functionalDepartmentCoopDefaultMapper.getCoopUsers(new Page<>(pageNum, pageSize),name ,orgIds);
    }
}