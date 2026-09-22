package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmission;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionUpdateCO;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
public interface AgentSubmissionService {
    AgentSubmission save(@Validated @NotNull AgentSubmissionCO co);

    void updateStatus(@NotNull Long id,@Validated @NotNull AgentSubmissionUpdateCO status);

    AgentSubmission getById(@NotNull Long id);

    IPage<AgentSubmission> page(@NotNull Integer currentPage, @NotNull Integer pageSize, @NotNull AgentSubmissionQO qo);
}
