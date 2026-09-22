package com.tdtech.cloudcmd.admin.resource.controller;

import com.tdtech.cloudcmd.admin.resource.entity.SystemConfig;
import com.tdtech.cloudcmd.admin.resource.service.ISystemConfigService;
import com.tdtech.cloudcmd.bean.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-03-10 13:57
 */
@Slf4j
@Tag(name = "警务协同系统配置")
@RestController
@RequestMapping("/admin/v1/system")
public class SystemConfigController {


    @Resource
    private ISystemConfigService systemConfigService;

    /**
     * 创建系统配置
     * @param systemConfig
     * @return
     */
    @PostMapping("/config")
    @Operation(summary = "创建系统配置")
    public R<Long> createSystemConfig(@Valid @RequestBody SystemConfig systemConfig) {
        return R.success(systemConfigService.createSystemConfig(systemConfig));
    }

    /**
     * 查询系统配置
     * @param
     * @return
     */
    @GetMapping("/config")
    @Operation(summary = "查询系统配置")
    public R<List<SystemConfig>> getSystemConfig() {
        return R.success(systemConfigService.listSystemConfig());
    }
    /**
     * 查询系统配置
     * @param
     * @return
     */
    @PutMapping("/config")
    @Operation(summary = "更新系统配置")
    public R<Boolean> updateSystemConfig(@RequestBody SystemConfig systemConfig) {
        systemConfigService.updateSystemConfig(systemConfig);
        return R.success(Boolean.TRUE);
    }
}
