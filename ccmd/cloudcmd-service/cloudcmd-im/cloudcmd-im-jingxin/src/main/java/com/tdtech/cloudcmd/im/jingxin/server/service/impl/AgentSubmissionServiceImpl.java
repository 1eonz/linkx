package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserGetVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmission;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionUpdateCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.AgentSubmissionService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.AgentSubmissionMapper;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

/**
 * AgentSubmission服务实现类
 */
@Slf4j
@Service
public class AgentSubmissionServiceImpl implements AgentSubmissionService {
    @Resource
    private AgentSubmissionMapper agentSubmissionMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient imHttpClient;

    /**
     * 保存AgentSubmission
     *
     */
    @Override
    public AgentSubmission save(@Validated @NotNull AgentSubmissionCO co) {
        var agentSubmission = BeanCopyUtils.copyBean(co, AgentSubmission::new);
        agentSubmission.setId(idWorker.nextId());
        agentSubmission.setStatus(AgentSubmission.AgentSubmissionStatusEnum.INIT);
        agentSubmission.setCreateTime(LocalDateTime.now());
        var userGetVo = imHttpClient.userPage(null, co.getFromId() + "");
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            log.warn("from user not found:{} {}", co.getFromId(), userGetVo);
            throw new BusinessException("from user not found:" + co.getFromId());
        }
        var imUser = userGetVo.getResults().get(0);
        var toId = imUser.getDirectLeaderId();
        if (toId == null || toId == 0L) {
            throw new BusinessException("没有配置直属领导");
        }
        var leader=imHttpClient.userPage(null, toId + "");
        Optional.ofNullable(leader).map(UserGetVo::getResults).map(a->a.get(0)).ifPresent(l->{
            agentSubmission.setToName(l.getName());
        });
        agentSubmission.setFromName(imUser.getName());
        agentSubmission.setToId(toId);
        var userDepartments = imUser.getUserDepartments();
        if (userDepartments != null && !userDepartments.isEmpty()) {
            userDepartments.stream().filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                agentSubmission.setFromDepId(dep.getId());
                agentSubmission.setFromDepName(dep.getDepartmentName());
            });
        }
        agentSubmissionMapper.insert(agentSubmission);
        return agentSubmission;
    }

    /**
     * 根据ID更新AgentSubmission
     *
     */
    @Override
    public void updateStatus(@NotNull Long id, @Validated @NotNull AgentSubmissionUpdateCO updateCO) {
        var update = agentSubmissionMapper.update(null, Wrappers.lambdaUpdate(AgentSubmission.class)//
            .set(AgentSubmission::getStatus, updateCO.getStatus().code())//
            .set(AgentSubmission::getReply, updateCO.getReply())//
            .set(AgentSubmission::getSubmissionTime, LocalDateTime.now())//
            .eq(AgentSubmission::getToId, updateCO.getOperId())//
            .eq(AgentSubmission::getId, id));
        if (update == 0) {
            throw new BusinessException("审批人不匹配");
        }
    }

    /**
     * AgentSubmission 根据ID查询AgentSubmission
     *
     * @param id 主键ID
     * @return AgentSubmission实体对象
     */
    @Override
    public AgentSubmission getById(@NotNull Long id) {
        return agentSubmissionMapper.selectById(id);
    }

    /**
     * 分页查询AgentSubmission
     *
     * @param currentPage 当前页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    @Override
    public IPage<AgentSubmission> page(@NotNull Integer currentPage, @NotNull Integer pageSize,
        @NotNull AgentSubmissionQO qo) {
        Page<AgentSubmission> page = new Page<>(currentPage, pageSize);
        var queryWrapper = Wrappers.lambdaQuery(AgentSubmission.class)//
            .eq(qo.getId() != null, AgentSubmission::getId, qo.getId())//
            .eq(qo.getFromId() != null, AgentSubmission::getFromId, qo.getFromId())//
            .eq(qo.getToId() != null, AgentSubmission::getToId, qo.getToId())//
            .ge(qo.getCreateTimeBegin() != null, AgentSubmission::getCreateTime, qo.getCreateTimeBegin())//
            .le(qo.getCreateTimeEnd() != null, AgentSubmission::getCreateTime, qo.getCreateTimeEnd())//
            .ge(qo.getSubmissionTimeBegin() != null, AgentSubmission::getCreateTime, qo.getSubmissionTimeBegin())//
            .le(qo.getSubmissionTimeEnd() != null, AgentSubmission::getCreateTime, qo.getSubmissionTimeEnd())//
            .le(qo.getAvailable() != null && qo.getAvailable(), AgentSubmission::getFromDate, LocalDate.now())//
            .ge(qo.getAvailable() != null && qo.getAvailable(), AgentSubmission::getToDate, LocalDate.now())//
            .eq(qo.getStatus() != null, AgentSubmission::getStatus, qo.getStatus())//
            .like(qo.getFromName() != null && !qo.getFromName().isBlank(), AgentSubmission::getFromName,
                qo.getFromName())//
            .like(qo.getFromDepName() != null && !qo.getFromDepName().isBlank(), AgentSubmission::getFromDepName,
                qo.getFromDepName())//
            .eq(qo.getFromDepId() != null, AgentSubmission::getFromDepId, qo.getFromDepId())//
            .apply(qo.getResourceType() != null, "{0} MEMBER OF (resources->'$[*].type')",
                qo.getResourceType())//
            .orderByDesc(AgentSubmission::getCreateTime) ;
        return agentSubmissionMapper.selectPage(page, queryWrapper);
    }

}
