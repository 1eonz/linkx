package com.chinasoft.cloud.module.aiagent.controller.app;

import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import com.chinasoft.cloud.module.aiagent.controller.app.co.ApproveCreatedCallbackCO;
import com.chinasoft.cloud.module.aiagent.controller.app.co.ApproveStatusCallbackCO;
import com.chinasoft.cloud.module.aiagent.service.AiAgentAskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.script.ScriptException;

@Slf4j
@Tag(name = "AI")
@RestController
@RequestMapping("/deepseek-zjk")
public class AiAgentOriginController {
    @Resource
    private AiAgentAskService aiAgentAskService;

    @PermitAll
    @TenantIgnore
    @Operation(summary = "AI审批建单回调")
    @PostMapping(value = "/xa/dk/approval/create/callback")
    public CommonResult<Boolean> approvalCreatedCallback(@Valid @RequestBody ApproveCreatedCallbackCO callbackCO) {
        aiAgentAskService.approveCreatedCallback(callbackCO);
        return CommonResult.success(true);
    }

    @PermitAll
    @TenantIgnore
    @Operation(summary = "AI审批状态回调")
    @PostMapping(value = "/xa/dk/approval/callback")
    public CommonResult<Boolean> approvalCallback(@Valid @RequestBody ApproveStatusCallbackCO callbackCO)
            throws ScriptException {
        aiAgentAskService.approveCallback(callbackCO);
        return CommonResult.success(true);
    }
}