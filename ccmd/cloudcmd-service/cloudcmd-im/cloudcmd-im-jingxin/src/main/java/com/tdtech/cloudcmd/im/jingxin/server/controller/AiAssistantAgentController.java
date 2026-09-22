package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.AiAssistantAgentDto;
import com.tdtech.cloudcmd.im.jingxin.server.service.AiAssistantAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Tag(name = "AI智能体绑定关系")
@RestController
@RequestMapping("/collaboration/v1/ai/assistant/agent")
@RequiredArgsConstructor
@Validated
public class AiAssistantAgentController {

    private final AiAssistantAgentService aiAssistantAgentService;

    @PostMapping
    @Operation(summary = "新增AI智能体绑定关系")
    public R<AiAssistantAgent> create(@RequestBody AiAssistantAgent aiAssistantAgent) {
        return R.success(aiAssistantAgentService.create(aiAssistantAgent));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询AI智能体绑定关系详情")
    public R<AiAssistantAgent> detail(@Parameter(description = "绑定关系ID") @PathVariable @NotNull Long id) {
        return R.success(aiAssistantAgentService.detail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询AI智能体绑定关系")
    public R<Page<AiAssistantAgent>> page(@RequestParam(defaultValue = "1") Long current,
                                          @RequestParam(defaultValue = "10") Long size,
                                          @RequestParam(required = false) Long virtualUserId,
                                          @RequestParam(required = false) Long agentId,
                                          @RequestParam(required = false) Long createdUserId) {
        return R.success(aiAssistantAgentService.page(current, size, virtualUserId, agentId, createdUserId));
    }

    @GetMapping("/list")
    @Operation(summary = "查询AI智能体绑定关系列表")
    public R<List<AiAssistantAgent>> list(@RequestParam(required = false) Long virtualUserId,
                                          @RequestParam(required = false) Long agentId,
                                          @RequestParam(required = false) Long createdUserId) {
        return R.success(aiAssistantAgentService.list(virtualUserId, agentId, createdUserId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新AI智能体绑定关系")
    public R<Boolean> update(@Parameter(description = "绑定关系ID") @PathVariable @NotNull Long id,
                             @RequestBody AiAssistantAgentDto aiAssistantAgent) {
        aiAssistantAgent.setId(id);
        return R.success(aiAssistantAgentService.update(aiAssistantAgent));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除AI智能体绑定关系")
    public R<Boolean> delete(@Parameter(description = "绑定关系ID") @PathVariable @NotNull Long id) {
        return R.success(aiAssistantAgentService.deleteById(id));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除AI智能体绑定关系")
    public R<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        return R.success(aiAssistantAgentService.deleteBatch(ids));
    }

    @GetMapping("/bind/list")
    @Operation(summary = "批量查询AI智能体与虚拟用户绑定关系")
    public R<List<ImUserVirtualRespVO>> selectBindUser(@RequestBody List<Long> ids) {
        return R.success(aiAssistantAgentService.selectBinsUser(ids));
    }
}
