package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiWsClientInterface;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.ImUserVirtual;
import com.tdtech.cloudcmd.im.jingxin.server.service.AiAssistantAgentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.AiAssistantAgentMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.cloudcmd.web.utils.ServletRequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantAgentServiceImpl extends ServiceImpl<AiAssistantAgentMapper, AiAssistantAgent>
        implements AiAssistantAgentService {

    @Resource
    private GroupAiWsClientInterface groupAiWsClient;

    @Resource
    private ImUserVirtualMapper imUserVirtualMapper;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private IdWorker idWorker;

    /**
     * 新增绑定关系。智能体和虚拟用户均只允许绑定一次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAssistantAgent create(AiAssistantAgent aiAssistantAgent) {
        validateForSave(aiAssistantAgent, false);
        aiAssistantAgent.setId(idWorker.nextId());
        if (aiAssistantAgent.getGmtCreated() == null) {
            aiAssistantAgent.setGmtCreated(new Date());
        }
        if (aiAssistantAgent.getCreatedUserId() == null) {
            aiAssistantAgent.setCreatedUserId(
                    Optional.ofNullable(SecurityUtils.getUser()).map(UserInfo::getUserId).orElse(0L)
            );
        }
        save(aiAssistantAgent);
        notifyAdd(Collections.singletonList(aiAssistantAgent.getVirtualUserId()));
        saveOperationLog(OperationTypeEnum.AGENT_INSERT, "新增AI智能体绑定关系：" + buildLogTarget(aiAssistantAgent));
        return aiAssistantAgent;
    }

    @Override
    public AiAssistantAgent detail(Long id) {
        validateId(id);
        AiAssistantAgent aiAssistantAgent = getById(id);
        if (aiAssistantAgent == null) {
            throw new BusinessException("智能体绑定关系不存在");
        }
        return aiAssistantAgent;
    }

    @Override
    public Page<AiAssistantAgent> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId) {
        long pageNum = current == null || current < 1 ? 1 : current;
        long pageSize = size == null || size < 1 ? 10 : size;
        return page(new Page<>(pageNum, pageSize), buildQueryWrapper(virtualUserId, agentId, createdUserId));
    }

    @Override
    public List<AiAssistantAgent> list(Long virtualUserId, Long agentId, Long createdUserId) {
        return list(buildQueryWrapper(virtualUserId, agentId, createdUserId));
    }

    /**
     * 更新绑定关系。换绑虚拟用户或智能体时，先移除旧连接，再建立新连接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(AiAssistantAgentDto aiAssistantAgentDto) {
        AiAssistantAgent aiAssistantAgent = BeanCopyUtils.copyBean(aiAssistantAgentDto, AiAssistantAgent::new);
        validateForSave(aiAssistantAgent, true);
        AiAssistantAgent old = detail(aiAssistantAgent.getId());
        if (aiAssistantAgent.getGmtCreated() == null) {
            aiAssistantAgent.setGmtCreated(old.getGmtCreated());
        }
        boolean result;
        if (aiAssistantAgent.getVirtualUserId() == null) {
            result = deleteById(aiAssistantAgent.getId());
        } else {
            result = updateByPrimaryKey(aiAssistantAgent) > 0;
        }
        if (result) {
            notifyUpdate(old, aiAssistantAgent);
            saveOperationLog(OperationTypeEnum.AGENT_UPDATE, "修改AI智能体绑定关系：" + buildLogTarget(aiAssistantAgent));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        validateId(id);
        AiAssistantAgent old = detail(id);
        boolean result = removeById(id);
        if (result) {
            notifyRemove(Collections.singletonList(old.getVirtualUserId()));
            saveOperationLog(OperationTypeEnum.AGENT_DELETE, "删除AI智能体绑定关系：" + buildLogTarget(old));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(distinctIds)) {
            throw new BusinessException("绑定关系ID不能为空");
        }
        List<AiAssistantAgent> oldList = listByIds(distinctIds);
        if (oldList.size() != distinctIds.size()) {
            throw new BusinessException("部分智能体绑定关系不存在");
        }
        List<Long> virtualUserIds = oldList.stream()
                .map(AiAssistantAgent::getVirtualUserId)
                .collect(Collectors.toList());
        boolean result = removeByIds(distinctIds);
        if (result) {
            notifyRemove(virtualUserIds);
            saveOperationLog(OperationTypeEnum.AGENT_DELETE, "批量删除AI智能体绑定关系：" + buildBatchLogTarget(oldList));
        }
        return result;
    }

    @Override
    public int updateByPrimaryKey(AiAssistantAgent aiAssistantAgent) {
        return baseMapper.updateByPrimaryKey(aiAssistantAgent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchInsert(List<AiAssistantAgent> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        validateBatchUnique(list);
        for (AiAssistantAgent aiAssistantAgent : list) {
            validateForSave(aiAssistantAgent, false);
            if (aiAssistantAgent.getGmtCreated() == null) {
                aiAssistantAgent.setGmtCreated(new Date());
            }
        }
        int result = baseMapper.batchInsert(list);
        if (result > 0) {
            notifyAdd(list.stream().map(AiAssistantAgent::getVirtualUserId).collect(Collectors.toList()));
            saveOperationLog(OperationTypeEnum.AGENT_INSERT, "批量新增AI智能体绑定关系：" + buildBatchLogTarget(list));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByPrimaryKeyIn(List<Long> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        List<Long> distinctIds = list.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(distinctIds)) {
            throw new BusinessException("绑定关系ID不能为空");
        }
        List<AiAssistantAgent> oldList = listByIds(distinctIds);
        List<Long> virtualUserIds = oldList.stream()
                .map(AiAssistantAgent::getVirtualUserId)
                .collect(Collectors.toList());
        int result = baseMapper.deleteByPrimaryKeyIn(distinctIds);
        if (result > 0) {
            notifyRemove(virtualUserIds);
            saveOperationLog(OperationTypeEnum.AGENT_DELETE, "批量删除AI智能体绑定关系：" + buildBatchLogTarget(oldList));
        }
        return result;
    }

    @Override
    public List<ImUserVirtualRespVO> selectBinsUser(List<Long> ids) {
        return baseMapper.selectBinsUser(ids);
    }

    private void saveOperationLog(OperationTypeEnum operationType, String operationInfo) {
        OperationLog operationLog = new OperationLog();
        operationLog.setOperator(operationType.getDesc());
        operationLog.setOperationResource(operationType.getParent().getName());
        operationLog.setOperation(operationInfo);
        try {
            operationLog.setIp(ServletRequestContext.getIp());
        } catch (Exception e) {
            log.error("获取ip失败", e);
        }
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
    }

    private String buildLogTarget(AiAssistantAgent aiAssistantAgent) {
        if (aiAssistantAgent == null) {
            return "AI智能体绑定";
        }
        StringBuilder sb = new StringBuilder("AI智能体 ");
        sb.append("[ id: ").append(aiAssistantAgent.getAgentId()).append(" ]");
        if (aiAssistantAgent.getVirtualUserId() != null) {
            sb.append(", 绑定虚拟用户 [ id: ").append(aiAssistantAgent.getVirtualUserId()).append(" ]");
        } else {
            sb.append(", 取消绑定虚拟用户");
        }

        return sb.toString();
    }

    private String buildBatchLogTarget(List<AiAssistantAgent> list) {
        if (CollectionUtils.isEmpty(list)) {
            return "AI智能体绑定关系";
        }
        return list.stream().map(this::buildLogTarget).collect(Collectors.joining(","));
    }

    private LambdaQueryWrapper<AiAssistantAgent> buildQueryWrapper(Long virtualUserId, Long agentId,
                                                                   Long createdUserId) {
        return Wrappers.lambdaQuery(AiAssistantAgent.class)
                .eq(virtualUserId != null, AiAssistantAgent::getVirtualUserId, virtualUserId)
                .eq(agentId != null, AiAssistantAgent::getAgentId, agentId)
                .eq(createdUserId != null, AiAssistantAgent::getCreatedUserId, createdUserId)
                .orderByDesc(AiAssistantAgent::getGmtCreated)
                .orderByDesc(AiAssistantAgent::getId);
    }

    private void validateForSave(AiAssistantAgent aiAssistantAgent, boolean update) {
        if (aiAssistantAgent == null) {
            throw new BusinessException("智能体绑定关系不能为空");
        }
        if (update) {
            validateId(aiAssistantAgent.getId());
        }
        if (!update && aiAssistantAgent.getVirtualUserId() == null) {
            throw new BusinessException("虚拟用户ID不能为空");
        }
        if (aiAssistantAgent.getAgentId() == null) {
            throw new BusinessException("AI智能体ID不能为空");
        }
        if (aiAssistantAgent.getCreatedUserId() == null) {
            throw new BusinessException("创建人ID不能为空");
        }
        if (aiAssistantAgent.getVirtualUserId() != null) {
            validateVirtualUser(aiAssistantAgent.getVirtualUserId());
            validateUnique(aiAssistantAgent);
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new BusinessException("绑定关系ID不能为空");
        }
    }

    private void validateVirtualUser(Long virtualUserId) {
        ImUserVirtual imUserVirtual = imUserVirtualMapper.selectById(virtualUserId);
        if (imUserVirtual == null || Objects.equals(imUserVirtual.getDeleted(), 1)) {
            throw new BusinessException("虚拟用户不存在或已删除");
        }
    }

    private void validateUnique(AiAssistantAgent aiAssistantAgent) {
        LambdaQueryWrapper<AiAssistantAgent> virtualUserWrapper = Wrappers.lambdaQuery(AiAssistantAgent.class)
                .eq(AiAssistantAgent::getVirtualUserId, aiAssistantAgent.getVirtualUserId())
                .ne(aiAssistantAgent.getId() != null, AiAssistantAgent::getId, aiAssistantAgent.getId());
        if (count(virtualUserWrapper) > 0) {
            throw new BusinessException("虚拟用户已绑定智能体");
        }

        LambdaQueryWrapper<AiAssistantAgent> agentWrapper = Wrappers.lambdaQuery(AiAssistantAgent.class)
                .eq(AiAssistantAgent::getAgentId, aiAssistantAgent.getAgentId())
                .ne(aiAssistantAgent.getId() != null, AiAssistantAgent::getId, aiAssistantAgent.getId());
        if (count(agentWrapper) > 0) {
            throw new BusinessException("AI智能体已绑定虚拟用户");
        }
    }

    private void validateBatchUnique(List<AiAssistantAgent> list) {
        long virtualUserCount = list.stream()
                .map(AiAssistantAgent::getVirtualUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (virtualUserCount != list.size()) {
            throw new BusinessException("批量绑定中存在重复或为空的虚拟用户");
        }
        long agentCount = list.stream()
                .map(AiAssistantAgent::getAgentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (agentCount != list.size()) {
            throw new BusinessException("批量绑定中存在重复或为空的AI智能体");
        }
    }

    private void notifyUpdate(AiAssistantAgent old, AiAssistantAgent current) {
        if (!Objects.equals(old.getVirtualUserId(), current.getVirtualUserId())
                || !Objects.equals(old.getAgentId(), current.getAgentId())) {
            notifyRemove(Collections.singletonList(old.getVirtualUserId()));
            if (current.getVirtualUserId() != null) {
                notifyAdd(Collections.singletonList(current.getVirtualUserId()));
            }
        }
    }

    private void notifyAdd(List<Long> virtualUserIds) {
        if (CollectionUtils.isEmpty(virtualUserIds)) {
            return;
        }
        List<AiVirtualUser> users = baseMapper.selectAiVirtualUserByVirtualUserIds(virtualUserIds);
        if (!CollectionUtils.isEmpty(users)) {
            groupAiWsClient.addGroupAi(users);
        }
    }

    private void notifyRemove(List<Long> virtualUserIds) {
        if (CollectionUtils.isEmpty(virtualUserIds)) {
            return;
        }
        List<ImUserVirtual> users = imUserVirtualMapper.selectAppIdByIds(virtualUserIds);
        if (!CollectionUtils.isEmpty(users)) {
            List<String> appIds = users.stream().map(ImUserVirtual::getAppId).collect(Collectors.toList());
            groupAiWsClient.removeGroupAi(appIds);
        }
    }
}
