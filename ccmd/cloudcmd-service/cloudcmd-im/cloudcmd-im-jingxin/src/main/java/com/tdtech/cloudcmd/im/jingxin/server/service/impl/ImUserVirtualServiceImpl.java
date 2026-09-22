package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualCreateReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiWsClientInterface;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.ImUserVirtual;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImUserVirtualService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.AiAssistantAgentMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ImUserVirtualServiceImpl extends ServiceImpl<ImUserVirtualMapper, ImUserVirtual> implements ImUserVirtualService {

    private static final Logger logger = LoggerFactory.getLogger(ImUserVirtualServiceImpl.class);

    @Resource
    GroupAiWsClientInterface groupAiWsClient;

    @Resource
    private IdWorker idWorker;

    @Resource
    private AiAssistantAgentMapper aiAssistantAgentMapper;

    private static final Integer DEFAULT_USER = 1;

    @Override
    @LogReport(type = OperationTypeEnum.VIRTUAL_USER_INSERT)
    public Long createVirtualUser(@LogReportParam(field = "userName") ImUserVirtualCreateReq req) {
        ImUserVirtual entity = BeanCopyUtils.copyBean(req, ImUserVirtual::new);
        checkVirtualUserExist(entity);
        entity.setCreatedBy(getCreateBy());
        long id = idWorker.nextId();
        entity.setId(id);
        save(entity);
        return id;
    }

    /**
     * 获取当前登录用户ID
     * @return  当前登录用户ID
     */
    private Long getCreateBy() {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new BusinessException("未获取到当前登录用户信息");
        }
        return user.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.VIRTUAL_USER_UPDATE)
    public void updateVirtualUser(Long id, @LogReportParam(field = "userName") ImUserVirtualUpdateReq req) {
        // 更新虚拟用户时，检查虚拟用户是否存在
        ImUserVirtual exist = checkVirtualUserExist(id);
        ImUserVirtual entity = BeanCopyUtils.copyBean(req, ImUserVirtual::new);
        entity.setId(id);
        entity.setDeleted(exist.getDeleted());
        checkVirtualUserExist(entity);
        updateByPrimaryKey(entity);
    }

    @Override
    public List<ImUserVirtualRespVO> listVirtualUsers(String userName) {
        LambdaQueryWrapper<ImUserVirtual> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserVirtual::getDeleted, Constant.VALID)
                .like(StringUtils.isNotBlank(userName), ImUserVirtual::getUserName, userName)
                .orderByDesc(ImUserVirtual::getUpdatedAt)
                .orderByDesc(ImUserVirtual::getCreatedAt);
        List<ImUserVirtual> list = list(wrapper);
        List<ImUserVirtualRespVO> imUserVirtualRespVOList = BeanCopyUtils.copyList(list, ImUserVirtualRespVO::new);
        bindAgentId(imUserVirtualRespVOList);
        return imUserVirtualRespVOList;
    }

    /**
     * 如果更新了默认入群用户，需要检查是否已存在默认入群用户
     * @param defaultUser 是否默认入群用户，用于一键建群拉默认智能体。0：不；1：要
     * @param excludeId 排除的虚拟用户ID
     */
    private void checkDefaultUserExist(Integer defaultUser, Long excludeId) {
        if (Objects.equals(defaultUser, DEFAULT_USER)) {
            LambdaQueryWrapper<ImUserVirtual> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ImUserVirtual::getDeleted, Constant.VALID)
                    .eq(ImUserVirtual::getDefaultUser, DEFAULT_USER)
                    .ne(Objects.nonNull(excludeId), ImUserVirtual::getId, excludeId);
            long count = count(wrapper);
            if (count > 0) {
                throw new BusinessException("默认虚拟用户已存在");
            }
        }
    }

    /**
     * 检查虚拟用户是否存在
     * @param id   虚拟用户ID
     * @return  虚拟用户实体
     */
    private ImUserVirtual checkVirtualUserExist(Long id) {
        ImUserVirtual existing = getById(id);
        if (existing == null || Objects.equals(existing.getDeleted(), Constant.DELETED)) {
            throw new BusinessException("虚拟用户不存在");
        }
        return existing;
    }

    private void checkVirtualUserExist(ImUserVirtual imUserVirtual) {
        ImUserVirtual existing = baseMapper.selectVirtualUserByUserName(imUserVirtual.getUserName());
        if (existing != null) {
            logger.info("old: {}, new: {}", existing, imUserVirtual);
            if (imUserVirtual.getId() != null && !Objects.equals(existing.getId(), imUserVirtual.getId())) {
                throw new BusinessException("虚拟用户名已存在");
            } else if (imUserVirtual.getId() == null) {
                throw new BusinessException("虚拟用户名已存在");
            }
        }
        existing = baseMapper.selectVirtualUserByAppId(imUserVirtual.getAppId());
        if (existing != null) {
            logger.info("old: {}, new: {}", existing, imUserVirtual);
            if (imUserVirtual.getId() != null && !Objects.equals(existing.getId(), imUserVirtual.getId())) {
                throw new BusinessException("虚拟用户ID已存在");
            } else if (imUserVirtual.getId() == null) {
                throw new BusinessException("虚拟用户ID已存在");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.VIRTUAL_USER_DELETE)
    public void deleteVirtualUser(@LogReportParam(field = "id") Long id) {
        // 删除虚拟用户时，需要检查虚拟用户是否存在
        var user = checkVirtualUserExist(id);
        this.deleteVirtualUserById(user);
    }

    @Override
    public int updateByPrimaryKey(ImUserVirtual imUserVirtual) {
        int result = baseMapper.updateByPrimaryKey(imUserVirtual);
        // 更新的虚拟用户可能绑定了agent，如果绑定了agent，更新时需要通知虚拟用户连接管理器新增相关用户的连接
        AiVirtualUser user = baseMapper.selectByClientId(imUserVirtual.getAppId());
        if (user != null) {
            groupAiWsClient.addGroupAi(user);
        }
        return result;
    }

    @Override
    public int batchInsert(List<ImUserVirtual> list) {
        // 新增的虚拟用户没绑定智能体，无需建立websocket通信
        return baseMapper.batchInsert(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByPrimaryKeyIn(List<Long> list) {
        List<ImUserVirtual> users = baseMapper.selectAppIdByIds(list);
        return this.deleteVirtualUserBatch(users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.VIRTUAL_USER_DELETE)
    public void deleteVirtualUserById(@LogReportParam(field = "userName") ImUserVirtual imUserVirtual) {
        // 删除时需要通知虚拟用户连接管理器删除相关用户的连接
        logger.info("deleteVirtualUserById: {}", imUserVirtual.getId());
        baseMapper.deleteUserById(imUserVirtual.getId());
        aiAssistantAgentMapper.deleteByVirtualUserId(imUserVirtual.getId());
        groupAiWsClient.removeGroupAi(imUserVirtual.getAppId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.VIRTUAL_USER_DELETE)
    public int deleteVirtualUserBatch(@LogReportParam(field = "userName") List<ImUserVirtual> imUserVirtuals) {
        // 删除时需要通知虚拟用户连接管理器删除相关用户的连接
        List<Long> ids = imUserVirtuals.stream().map(ImUserVirtual::getId).collect(Collectors.toList());
        List<String> appIds = imUserVirtuals.stream().map(ImUserVirtual::getAppId).collect(Collectors.toList());
        int result = baseMapper.deleteByPrimaryKeyIn(ids);
        aiAssistantAgentMapper.deleteByVirtualUserIds(ids);
        groupAiWsClient.removeGroupAi(appIds);
        return result;
    }

    private void bindAgentId(List<ImUserVirtualRespVO> imUserVirtualRespVOList) {
        if (CollectionUtils.isNotEmpty(imUserVirtualRespVOList)) {
            List<AiAssistantAgent> relAgentList = aiAssistantAgentMapper.selectList(
                    Wrappers.lambdaQuery(AiAssistantAgent.class)
                            .select(AiAssistantAgent::getAgentId, AiAssistantAgent::getVirtualUserId)
                            .in(
                                    AiAssistantAgent::getVirtualUserId,
                                    imUserVirtualRespVOList.stream().map(ImUserVirtualRespVO::getId).collect(Collectors.toSet())
                            )
            );
            Map<Long, Long> virtualUserId2AgentIdMap = relAgentList.stream().collect(Collectors.toMap(
                    AiAssistantAgent::getVirtualUserId,
                    AiAssistantAgent::getAgentId,
                    (k1, k2) -> k1
            ));
            for (ImUserVirtualRespVO imUserVirtualRespVO : imUserVirtualRespVOList) {
                imUserVirtualRespVO.setAgentId(virtualUserId2AgentIdMap.get(imUserVirtualRespVO.getId()));
            }
        }
    }
}