package com.tdtech.cloudcmd.admin.resource.controller;

import com.tdtech.cloudcmd.admin.annotation.OperationAnnotation;
import com.tdtech.cloudcmd.admin.resource.entity.IcpConfig;
import com.tdtech.cloudcmd.admin.resource.entity.vo.IcpConfigVO;
import com.tdtech.cloudcmd.admin.resource.service.IIcpConfigService;
import com.tdtech.cloudcmd.bean.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Tag(name = "融合通信配置管理", description = "融合通信对接配置管理相关接口")
@RestController
@RequestMapping("/admin/v1/icp/server")
@Slf4j
public class IcpConfigController {

    @Resource
    private IIcpConfigService icpConfigService;

    @PutMapping("/config")
    @Operation(summary = "更新服务器配置", description = "更新服务器配置")
    @ApiResponse(responseCode = "200", description = "设置成功", content = @Content(schema = @Schema(implementation = R.class)))
    @OperationAnnotation(value = "OPERATION_ANNOTATION_60",
            bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.IcpConfig")
    public R<Void> config(@RequestBody @Valid @NotNull IcpConfigVO icpConfigVO) {
        icpConfigService.updateIcpConfig(icpConfigVO);
        return R.success();
    }

    @GetMapping("/config")
    @Operation(summary = "获取服务器配置", description = "获取服务器配置")
    @ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = R.class)))
    public R<IcpConfigVO> config() {
        IcpConfig icpConfig = icpConfigService.getIcpConfig();
        IcpConfigVO icpConfigVO = new IcpConfigVO();
        BeanUtils.copyProperties(icpConfig, icpConfigVO);
        return R.success(icpConfigVO);
    }

}
