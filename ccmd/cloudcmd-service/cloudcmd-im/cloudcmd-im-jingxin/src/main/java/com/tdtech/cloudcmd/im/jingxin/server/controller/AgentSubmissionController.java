package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmission;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentSubmissionUpdateCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.AgentSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Tag(name = "申请")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/agent/submission")
@RequiredArgsConstructor
@Validated
public class AgentSubmissionController {
    private final AgentSubmissionService agentSubmissionService;
    @Operation(summary = "创建申请")
    @PostMapping
    public R<AgentSubmission> save(@RequestBody @Valid @NotNull AgentSubmissionCO co) {
        return R.success(agentSubmissionService.save(co));
    }

    @Operation(summary = "更新申请状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "申请ID") @PathVariable @NotNull Long id,
        @RequestBody AgentSubmissionUpdateCO updateCO) {
        agentSubmissionService.updateStatus(id, updateCO);
        return R.success();
    }

    @Operation(summary = "根据ID查询申请详情")
    @GetMapping("/{id}")
    public R<AgentSubmission> getById(@Parameter(description = "申请ID") @PathVariable @NotNull Long id) {
        AgentSubmission result = agentSubmissionService.getById(id);
        return R.success(result);
    }

    @Operation(summary = "分页查询申请列表")
    @GetMapping("/page")
    public R<IPage<AgentSubmission>> page(
        @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") @NotNull Integer currentPage,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") @NotNull Integer pageSize,
        @Valid AgentSubmissionQO qo) {
        IPage<AgentSubmission> result = agentSubmissionService.page(currentPage, pageSize, qo);
        return R.success(result);
    }

}
