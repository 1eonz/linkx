package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.FunctionalDepartmentMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 职能部门服务实现类
 */
@Slf4j
@Service
public class FunctionalDepartmentServiceImpl extends ServiceImpl<FunctionalDepartmentMapper, FunctionalDepartment>
        implements FunctionalDepartmentService {


    @Resource
    private IdWorker idWorker;

    @Resource
    private FunctionalDepartmentMapper functionalDepartmentMapper;

    @Resource
    private ReportUtil reportUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createFunctionalDepartmentNode(FunctionalDepartmentReq functionalDepartmentReq) {
        log.info("创建职能部门节点，参数：{}", functionalDepartmentReq);
        
        // 参数校验
        if (StringUtils.isBlank(functionalDepartmentReq.getName())) {
            throw new BusinessException("部门名称不能为空");
        }

        int depth = levelDepth(functionalDepartmentReq.getParentId());

        if (depth > 5) {
            throw new BusinessException("最多支持五级节点");
        }

        if (functionalDepartmentReq.getParentId() != 0L) {
            FunctionalDepartment functionalDepartment = functionalDepartmentMapper.selectByParentId(functionalDepartmentReq.getParentId());
            if (functionalDepartment == null) {
                throw new BusinessException("父节点不存在");
            }
        }

        int count = functionalDepartmentMapper.selectByNameCount(functionalDepartmentReq.getName());
        if (count > 0) {
            throw new BusinessException("节点名称已存在");
        }
        if(functionalDepartmentReq.getParentId() != 0L){
            count = functionalDepartmentMapper.selectChildrenCount(functionalDepartmentReq.getParentId());
            if (count >= 10) {
                throw new BusinessException("每个职能分类最多绑定10个子部门");
            }
        }
        // 创建部门实体
        FunctionalDepartment functionalDepartment = new FunctionalDepartment();
        functionalDepartment.setId(idWorker.nextId());
        functionalDepartment.setName(functionalDepartmentReq.getName());
        functionalDepartment.setParentId(functionalDepartmentReq.getParentId());
        functionalDepartment.setCreator(functionalDepartmentReq.getCreator());
        functionalDepartment.setIsDeleted(0);

        // 保存部门
        boolean result = save(functionalDepartment);
        
        log.info("创建职能部门节点完成，结果：{}", result);
        return result;
    }

    @Override
    public Boolean updateFunctionalDepartmentNode(String departmentId, FunctionalDepartmentReq functionalDepartmentReq) {
        log.info("修改职能部门节点，部门ID：{}，参数：{}", departmentId, functionalDepartmentReq);
        
        // 参数校验
        if (StringUtils.isBlank(departmentId)) {
            log.warn("部门ID不能为空");
            return false;
        }

        if (StringUtils.isBlank(functionalDepartmentReq.getName())) {
            log.warn("部门名称不能为空");
            return false;
        }

        // 查询部门是否存在
        FunctionalDepartment existingDepartment = getById(departmentId);
        if (existingDepartment == null) {
            log.warn("部门不存在，部门ID：{}", departmentId);
            return false;
        }

        FunctionalDepartment oldDepartment = new FunctionalDepartment();
        oldDepartment.setName(existingDepartment.getName());
        oldDepartment.setParentId(existingDepartment.getParentId());

        boolean needUpdate = false;


        if (functionalDepartmentReq.getParentId() != null && !existingDepartment.getParentId().equals(functionalDepartmentReq.getParentId())
                && functionalDepartmentReq.getParentId() != 0L) {
            FunctionalDepartment functionalDepartment = functionalDepartmentMapper.selectByParentId(functionalDepartmentReq.getParentId());
            if (functionalDepartment == null) {
                throw new BusinessException("父节点不存在");
            } else {
                existingDepartment.setParentId(functionalDepartmentReq.getParentId());
                needUpdate = true;
            }
        }

        if (StringUtils.isNotEmpty(functionalDepartmentReq.getName())
                && !existingDepartment.getName().equals(functionalDepartmentReq.getName())) {
            int count = functionalDepartmentMapper.selectByNameCount(functionalDepartmentReq.getName());
            if (count > 0) {
                throw new BusinessException("节点名称已存在");
            } else {
                existingDepartment.setName(functionalDepartmentReq.getName());
                needUpdate = true;
            }
        }
        // 更新部门信息
        if(needUpdate){
            // 更新部门
            updateById(existingDepartment);
            log.info("修改职能部门节点完成，结果：{}", true);
            List<String> changes = buildChangeLog(oldDepartment, existingDepartment);
            saveOperationLog(changes);
            return true;
        }
        return false;
    }

    @Override
    public Boolean deleteFunctionalDepartmentNode(String departmentId) {
        log.info("删除职能部门节点，部门ID：{}", departmentId);
        
        // 参数校验
        if (StringUtils.isBlank(departmentId)) {
            throw new BusinessException("部门ID不能为空");
        }

        // 查询部门是否存在
        FunctionalDepartment existingDepartment = getById(departmentId);
        if (existingDepartment == null) {
            throw new BusinessException("部门不存在");
        }

        // 查询是否有子部门
        LambdaQueryWrapper<FunctionalDepartment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionalDepartment::getParentId, departmentId);
        long childCount = count(queryWrapper);
        
        if (childCount > 0) {
            throw new BusinessException("该部门下存在子部门，无法删除");
        }

        // 删除部门（逻辑删除）
        UpdateWrapper<FunctionalDepartment> set = new UpdateWrapper<FunctionalDepartment>()
                .eq("id", departmentId)
                .set("is_deleted", 1);
        // 记录删除日志
        OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_DELETE);
        log.setOperation(String.format(log.getOperation(), existingDepartment.getName()));
        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            log.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(log);
        return update(set);
    }

    @Override
    public List<FunctionalDepartment> childrenDepartmentNode(String departmentId) {
        log.info("查询职能部门的子部门，部门ID：{}", departmentId);
        
        // 参数校验
        if (StringUtils.isBlank(departmentId)) {
            throw new BusinessException("部门ID不能为空");
        }

        // 检查节点是否存在（除了根节点 "0"）
        if (!"0".equals(departmentId)) {
            FunctionalDepartment department = functionalDepartmentMapper.selectInfoById(departmentId);
            if (department == null) {
                throw new BusinessException("节点不存在");
            }
        }

        // 查询子部门列表
        List<FunctionalDepartment> nodes = functionalDepartmentMapper.selectChildren(Long.parseLong(departmentId));

        // 计算每个节点是否有子节点
        if (CollectionUtils.isNotEmpty(nodes)) {
            // 获取所有子部门的ID
            List<Long> ids = nodes.stream()
                    .map(FunctionalDepartment::getId)
                    .collect(Collectors.toList());
            
            // 批量查询哪些节点有子节点
            List<FunctionalDepartment> childrenNodes = functionalDepartmentMapper.selectAllChildrenCount(ids);
            
            // 构建Map：部门ID -> 是否有子节点
            Map<Long, Boolean> map = childrenNodes.stream()
                    .filter(n -> !Objects.isNull(n.getHasChildren()))
                    .collect(Collectors.toMap(FunctionalDepartment::getId, FunctionalDepartment::getHasChildren));
            
            // 设置每个节点的 hasChildren 属性
            nodes.forEach(n -> n.setHasChildren(map.getOrDefault(n.getId(), false)));
        }

        log.info("查询到子部门数量：{}", nodes.size());
        return nodes;
    }

    private int levelDepth(Long levelId) {
        int depth = 1;
        Long currentId = levelId;

        while (!currentId.equals(0L)) {
            FunctionalDepartment functionalDepartment = functionalDepartmentMapper.selectById(currentId);
            if (functionalDepartment == null) {
                break;
            }

            currentId = functionalDepartment.getParentId();
            depth++;
        }

        return depth;
    }

    private List<String> buildChangeLog(FunctionalDepartment oldDepartment, FunctionalDepartment newDepartment) {
        List<String> changes = new ArrayList<>();
        changes.add(formatChangeLog("FUNCTIONAL_DEPARTMENT_NAME", oldDepartment.getName(), newDepartment.getName()));
        // 比较父部门ID
        if (!Objects.equals(oldDepartment.getParentId(), newDepartment.getParentId())) {
            String oldParentName = getParentDepartmentName(oldDepartment.getParentId());
            String newParentName = getParentDepartmentName(newDepartment.getParentId());
            changes.add(formatChangeLog("FUNCTIONAL_DEPARTMENT_PARENT", oldParentName, newParentName));
        }
        
        return changes;
    }

    private String getParentDepartmentName(Long parentId) {
        if (parentId == null || parentId.equals(0L)) {
            return I18nUtil.get("FUNCTIONAL_DEPARTMENT_ROOT");
        }
        FunctionalDepartment parent = functionalDepartmentMapper.selectById(parentId);
        return parent != null ? parent.getName() : I18nUtil.get("FUNCTIONAL_DEPARTMENT_UNKNOWN");
    }

    private String formatChangeLog(String fieldKey, Object oldValue, Object newValue) {
        String fieldName = I18nUtil.get(fieldKey);
        return fieldName + "：" + oldValue + "->" + newValue;
    }

    private void saveOperationLog(List<String> changes) {
        try {
            OperationLog operationLog = new OperationLog(OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_UPDATE);
            operationLog.setOperation(String.format(operationLog.getOperation(), String.join(",", changes)));
            log.info("保存操作日志：{}", operationLog.getOperation());
            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                operationLog.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(operationLog);
        } catch (Exception e) {
            log.error("saveOperationLog error: {}", e.getMessage());
        }
    }
}