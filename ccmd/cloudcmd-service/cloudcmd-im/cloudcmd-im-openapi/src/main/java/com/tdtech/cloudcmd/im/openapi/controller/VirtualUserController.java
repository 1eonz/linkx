package com.tdtech.cloudcmd.im.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.VirtualUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.ImUserVirtualVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.VirtualUserBindAgentVO;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@Tag(name = "虚拟用户相关接口", description = "虚拟用户相关接口")
@RestController
@RequestMapping("/openapi/v1/virtual-user")
@RequiredArgsConstructor
@Validated
@OpenApiOauth(BusinessScopeEnum.AGENT_USER)
@RequestLimit(business = "虚拟用户相关接口")
public class VirtualUserController {

    @DubboReference
    private VirtualUserRpcApi virtualUserRpcApi;

    @Operation(summary = "获取虚拟用户列表", description = "获取虚拟用户列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/virtual")
    public R<List<ImUserVirtualVO>> count(
            @Parameter(name = "userName", description = "虚拟用户名", in = ParameterIn.QUERY)
            @RequestParam(value = "userName", required = false) String userName) {
        return R.success(virtualUserRpcApi.listVirtualUsers(userName));
    }

    @Operation(summary = "获取agent绑定的虚拟用户列表", description = "获取agent绑定的虚拟用户列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @PostMapping("/bind/list")
    public R<List<ImUserVirtualVO>> bindUser(
            @Parameter(name = "ids", description = "agentId列表")
            @RequestBody List<Long> ids) {
        return R.success(virtualUserRpcApi.selectBinsUser(ids));
    }

    @Operation(summary = "分页查询AI智能体绑定关系", description = "分页查询AI智能体绑定关系")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/agent/page")
    public R<Page<VirtualUserBindAgentVO>> page(@Parameter(name = "current", description = "当前页码") @RequestParam(defaultValue = "1") Long current,
                                                @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "10") Long size,
                                                @Parameter(name = "virtualUserId", description = "虚拟用户id") @RequestParam(required = false) Long virtualUserId,
                                                @Parameter(name = "agentId", description = "智能体id") @RequestParam(required = false) Long agentId,
                                                @Parameter(name = "createdUserId", description = "当前用户id") @RequestParam(required = false) Long createdUserId) {
        return R.success(virtualUserRpcApi.page(current, size, virtualUserId, agentId, createdUserId));
    }

    @PutMapping("/agent/{id}")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @Operation(summary = "更新AI智能体绑定关系")
    public R<Boolean> update(@Parameter(description = "绑定关系ID") @PathVariable @NotNull Long id,
                             @RequestBody AiAssistantAgentDto aiAssistantAgent) {
        aiAssistantAgent.setId(id);
        return R.success(virtualUserRpcApi.update(aiAssistantAgent));
    }

    @PostMapping("/agent")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @Operation(summary = "新增AI智能体绑定关系")
    public R<AiAssistantAgentDto> create(@RequestBody AiAssistantAgentDto aiAssistantAgent) {
        return R.success(virtualUserRpcApi.create(aiAssistantAgent));
    }
}
