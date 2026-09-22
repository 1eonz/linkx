package com.chinasoft.cloud.module.aiagent.controller.admin;

import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.GlobalCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertAiSettingsCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiSettingsVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.GlobalVO;
import com.chinasoft.cloud.module.aiagent.service.GlobalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI系统设置")
@RestController
@RequestMapping("/proxy/ai/v1/aiagent/management/settings")
public class AiAgentSettingsController {

    @Resource
    private GlobalsService globalsService;

    @PermitAll
    @GetMapping
    @Operation(summary = "获取系统设置")
    public CommonResult<AiSettingsVO> getSettings() {
        return CommonResult.success(globalsService.getAiSettings());
    }

    @PermitAll
    @GetMapping("/globals")
    @Operation(summary = "获取系统设置")
    public CommonResult<List<GlobalVO>> getGlobalSettings(@Parameter(name = "name", description = "全局配置名称") @RequestParam(required = false) String name) {
        return CommonResult.success(globalsService.getGlobalSettings(name));
    }

    @PermitAll
    @PutMapping
    @Operation(summary = "更新系统设置")
    public CommonResult<Void> updateSettings(@Valid @RequestBody UpsertAiSettingsCO settingsCO) {
        globalsService.updateAiSettings(settingsCO);
        return CommonResult.success(null);
    }

    @PermitAll
    @PostMapping("/globals")
    @Operation(summary = "更新系统设置")
    public CommonResult<Boolean> updateGlobalSettings(@RequestBody GlobalCO globalCO) {
        return CommonResult.success(globalsService.updateGlobalSettings(globalCO));
    }
}
