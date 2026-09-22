package com.chinasoft.cloud.module.aiagent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.UserVirtualVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.VirtualUserBindAgentVO;
import com.chinasoft.cloud.module.aiagent.service.VirtualUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI全局变量")
@RestController
@RequestMapping("/proxy/ai/v1/aiagent/virtual/user")
public class AiAgentVirtualUserController {

    @Resource
    private VirtualUserService virtualUserService;

    @PermitAll
    @GetMapping("/virtual")
    @Operation(summary = "获取虚拟用户列表", description = "获取虚拟用户列表")
    public CommonResult<List<UserVirtualVO>> count(
            @Parameter(name = "userName", description = "虚拟用户名")
            @RequestParam(value = "userName", required = false) String userName) {
        return CommonResult.success(virtualUserService.listVirtualUsers(userName));
    }

    @PermitAll
    @PostMapping("/bind/list")
    @Operation(summary = "获取agent绑定的虚拟用户列表", description = "获取agent绑定的虚拟用户列表")
    public CommonResult<List<UserVirtualVO>> bindUser(
            @Parameter(name = "ids", description = "agentId列表")
            @RequestBody List<Long> ids) {
        return CommonResult.success(virtualUserService.selectBinsUser(ids));
    }

    @PermitAll
    @GetMapping("/agent/page")
    @Operation(summary = "分页查询AI智能体绑定关系", description = "分页查询AI智能体绑定关系")
    public CommonResult<Page<VirtualUserBindAgentVO>> page(@Parameter(name = "current", description = "当前页码") @RequestParam(defaultValue = "1") Long current,
                                                           @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "10") Long size,
                                                           @Parameter(name = "virtualUserId", description = "虚拟用户id") @RequestParam(required = false) Long virtualUserId,
                                                           @Parameter(name = "agentId", description = "智能体id") @RequestParam(required = false) Long agentId,
                                                           @Parameter(name = "createdUserId", description = "当前用户id") @RequestParam(required = false) Long createdUserId) {
        return CommonResult.success(virtualUserService.page(current, size, virtualUserId, agentId, createdUserId));
    }

    @PermitAll
    @PutMapping("/agent/{id}")
    @Operation(summary = "更新AI智能体绑定关系")
    public CommonResult<Boolean> update(@Parameter(description = "绑定关系ID") @PathVariable Long id,
                                        @RequestBody AiAssistantAgentDto aiAssistantAgent) {
        aiAssistantAgent.setId(id);
        return CommonResult.success(virtualUserService.update(aiAssistantAgent));
    }

    @PermitAll
    @PostMapping("/agent")
    @Operation(summary = "新增AI智能体绑定关系")
    public CommonResult<AiAssistantAgentDto> create(@RequestBody AiAssistantAgentDto aiAssistantAgent) {
        return CommonResult.success(virtualUserService.create(aiAssistantAgent));
    }
}
