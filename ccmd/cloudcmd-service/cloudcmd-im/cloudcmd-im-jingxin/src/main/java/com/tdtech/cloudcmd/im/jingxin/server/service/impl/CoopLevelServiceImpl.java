package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevel;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelDO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelMemberDO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.CoopLevelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CoopLevelMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CoopLevelMemberMapper;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CoopLevelServiceImpl extends ServiceImpl<CoopLevelMapper, CoopLevelDO> implements CoopLevelService {

    @Resource
    CoopLevelMapper coopLevelMapper;

    @Resource
    CoopLevelMemberMapper coopLevelMemberMapper;

    @Resource
    ReportUtil reportUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.COOP_LEVEL_INSERT)
    public boolean createCoopLevelNode(@LogReportParam(field = "name") CoopLevelReq coopLevelReq) {
        if (coopLevelReq.getParentId() == null) {
            coopLevelReq.setParentId(0L);
        }

        if (StringUtils.isEmpty(coopLevelReq.getName())) {
            throw new BusinessException("节点名称不能为空");
        }

        int depth = levelDepth(coopLevelReq.getParentId());

        if (depth > 5) {
            throw new BusinessException("最多支持五级节点");
        }

        if (coopLevelReq.getParentId() != 0L) {
            CoopLevelDO coopLevelDO = coopLevelMapper.selectByParentId(coopLevelReq.getParentId());
            if (coopLevelDO == null) {
                throw new BusinessException("父节点不存在");
            }
        }

        int count = coopLevelMapper.selectByNameCount(coopLevelReq.getName());
        if (count > 0) {
            throw new BusinessException("节点名称已存在");
        }

        if (coopLevelReq.getParentId() != 0L) {
            count = coopLevelMapper.selectChildrenCount(coopLevelReq.getParentId());
            if (count >= 10) {
                throw new BusinessException("每个层级最多绑定10个子层级节点");
            }
        }

        CoopLevelDO coopLevelDO = new CoopLevelDO();
        coopLevelDO.setParentId(coopLevelReq.getParentId());
        coopLevelDO.setName(coopLevelReq.getName());
        coopLevelMapper.insert(coopLevelDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCoopLevelNode(String levelId, CoopLevelReq coopLevelReq) {

        if (StringUtils.isEmpty(coopLevelReq.getName())) {
            throw new BusinessException("节点名称不能为空");
        }

        CoopLevelDO coopLevelDO = coopLevelMapper.selectInfoById(levelId);
        if (coopLevelDO == null) {
            throw new BusinessException("节点不存在");
        }

        String oldCoopLevelName = coopLevelDO.getName();

        boolean needUpdate = false;

        if (coopLevelReq.getParentId() != null && !coopLevelDO.getParentId().equals(coopLevelReq.getParentId()) && coopLevelReq.getParentId() != 0L) {
            CoopLevelDO parentCoopLevelDO = coopLevelMapper.selectByParentId(coopLevelReq.getParentId());
            if (parentCoopLevelDO == null) {
                throw new BusinessException("父节点不存在");
            } else {
                coopLevelDO.setParentId(coopLevelReq.getParentId());
                needUpdate = true;
            }
        }

        if (StringUtils.isNotEmpty(coopLevelReq.getName()) && !coopLevelDO.getName().equals(coopLevelReq.getName())) {
            int count = coopLevelMapper.selectByNameCount(coopLevelReq.getName());
            if (count > 0) {
                throw new BusinessException("节点名称已存在");
            } else {
                coopLevelDO.setName(coopLevelReq.getName());
                needUpdate = true;
            }
        }

        if (needUpdate) {
            coopLevelMapper.updateById(coopLevelDO);
            OperationLog operationLog = new OperationLog(OperationTypeEnum.COOP_LEVEL_UPDATE);
            operationLog.setOperation(String.format(operationLog.getOperation(), oldCoopLevelName, coopLevelReq.getName()));
            UserInfo user = SecurityUtils.getUser();
            if (user != null) {
                operationLog.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(operationLog);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCoopLevelNode(String levelId) {
        CoopLevelDO coopLevelDO = coopLevelMapper.selectInfoById(levelId);
        if (coopLevelDO == null) {
            throw new BusinessException("节点不存在");
        }

        Long levelIdNum;

        try {
            levelIdNum = Long.parseLong(levelId);
        } catch (NumberFormatException e) {
            throw new BusinessException("节点id不正确");
        }

        int count = coopLevelMapper.selectChildrenCount(levelIdNum);
        if (count > 0) {
            throw new BusinessException("节点下有子节点，不能删除节点");
        }

        List<CoopLevelMemberDO> coopLevelMembers = coopLevelMemberMapper.getMemberByLevelId(levelIdNum);
        if (!coopLevelMembers.isEmpty()) {
            List<String> ids = coopLevelMembers.stream().map(m -> String.valueOf(m.getId())).collect(Collectors.toList());
            coopLevelMemberMapper.deleteByCoopUserIds(ids);
        }

        coopLevelDO.setIsDeleted(1);

        coopLevelMapper.updateById(coopLevelDO);

        OperationLog operationLog = new OperationLog(OperationTypeEnum.COOP_LEVEL_DELETE);
        operationLog.setOperation(String.format(operationLog.getOperation(), coopLevelDO.getName()));
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CoopLevel> childrenLevelNode(String levelId) {
        if (!"0".equals(levelId)) {
            CoopLevelDO coopLevelDO = coopLevelMapper.selectInfoById(levelId);
            if (coopLevelDO == null) {
                throw new BusinessException("节点不存在");
            }
        }

        List<CoopLevel> nodes = coopLevelMapper.selectChildren(Long.parseLong(levelId));

        if (CollectionUtils.isNotEmpty(nodes)) {
            List<Long> ids = nodes.stream().map(CoopLevel::getId).collect(Collectors.toList());
            List<CoopLevel> childrenNodes = coopLevelMapper.selectAllChildrenCount(ids);
            Map<Long, Boolean> map = childrenNodes.stream().filter(n -> !Objects.isNull(n.getHasChildren()))
                    .collect(Collectors.toMap(CoopLevel::getId, CoopLevel::getHasChildren));
            nodes.forEach(n -> n.setHasChildren(map.getOrDefault(n.getId(), false)));
        }

        return nodes;
    }

    private int levelDepth(Long levelId) {
        int depth = 1;
        Long currentId = levelId;

        while (!currentId.equals(0L)) {
            CoopLevelDO coopLevelDO = coopLevelMapper.selectById(currentId);
            if (coopLevelDO == null) {
                break;
            }

            currentId = coopLevelDO.getParentId();
            depth++;
        }

        return depth;
    }
}
