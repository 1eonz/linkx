package com.tdtech.cloudcmd.icp.proxy.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DelMembersParamsVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DynamicGroupQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DynamicGroupVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.ExitLocationShareVO;
import com.tdtech.cloudcmd.icp.proxy.entity.DynamicGroupMember;
import com.tdtech.cloudcmd.icp.proxy.service.DynamicGroupService;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.web.advice.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "动态群组管理", description = "动态群组相关接口")
@RestController
@RequestMapping("/proxy/icp/v1/dynamic-group")
@Slf4j
public class DynamicGroupController {

    @Resource
    private DynamicGroupService dynamicGroupService;

    @PostMapping("/upsert")
    @Operation(summary = "创建或更新动态群组", description = "用于创建新动态群组或更新现有动态群组信息")
    public R<Void> upsertGroup(
        @Parameter(description = "动态群组信息", required = true) @Valid @RequestBody DynamicGroupVO dynamicGroupVO) {
        dynamicGroupService.upsertGroup(dynamicGroupVO);
        return R.success();
    }

    @PostMapping("/exit-location-share")
    @Operation(summary = "统一退出位置共享", description = "编排三步操作：群主重置按钮状态、退出位置共享、删除动态群组成员")
    public R<Void> exitLocationShare(
        @Parameter(description = "退出位置共享请求", required = true) @Valid @RequestBody ExitLocationShareVO vo) {
        log.info("统一退出位置共享, shareId={}, groupId={}, userId={}, isOwner={}",
            vo.getShareId(), vo.getGroupId(), vo.getUserId(), vo.getIsOwner());
        dynamicGroupService.exitLocationShare(vo);
        return R.success();
    }

    @PutMapping("/{groupId}/add-member")
    @Operation(summary = "添加群组成员", description = "向指定动态群组添加成员")
    public R<Void> addMember(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable String groupId,
        @Parameter(description = "群组成员列表",
            required = true) @RequestBody List<DynamicGroupMember> dynamicGroupMembers) {
        var exists = dynamicGroupService.groupInfo(groupId);
        if (exists == null) {
            return R.failure("群组不存在");
        }
        log.info("页面调用拉起接口");
        dynamicGroupService.addMember(groupId, exists.getGroupName(), exists.getOwnerId(), dynamicGroupMembers);
        return R.success();
    }

    @PutMapping("/{groupId}/del-members")
    @Operation(summary = "删除群组成员", description = "从指定动态群组删除成员")
    public R<Void> delMembers(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable String groupId,
        @Parameter(description = "用户ID列表", required = true) @RequestBody DelMembersParamsVO delMembersParamsVO) {
        List<String> userIds = delMembersParamsVO.getUserIds();
        Integer isOwner = delMembersParamsVO.getIsOwner();
        if (userIds == null || userIds.isEmpty()) {
            throw new SystemException("members cant be empty");
        }
        log.info("页面调用删除接口，userIds: {}, isOwner: {}", userIds, isOwner);
        dynamicGroupService.delMembers(groupId, userIds ,isOwner);
        return R.success();
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "删除群组", description = "删除群组")
    public R<Void> delGroup(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable String groupId) {
        dynamicGroupService.delMembers(groupId, null,0);
        return R.success();
    }

    @GetMapping("/{groupId}/info")
    @Operation(summary = "获取群组信息", description = "获取指定动态群组的详细信息")
    public R<DynamicGroupVO> groupInfo(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable String groupId) {
        return R.success(dynamicGroupService.groupInfo(groupId));
    }

    @GetMapping("/list")
    @Operation(summary = "获取群组列表", description = "根据查询条件获取动态群组分页列表")
    public R<CcmdPage<DynamicGroupVO>> listGroup(@Valid DynamicGroupQO dynamicGroupQO) {
        return R.success(dynamicGroupService.listGroup(dynamicGroupQO));
    }

    @GetMapping("/{groupId}/button")
    @Operation(summary = "按钮状态", description = "按钮状态")
    public R<String> getButton(@PathVariable String groupId) {
        var button = dynamicGroupService.getButton(groupId);
        return R.success(button == null ? "true" : button);
    }

    @PutMapping("/{groupId}/button/{status}")
    @Operation(summary = "更新按钮状态", description = "更新按钮状态")
    public R<Void> updateButton(@PathVariable("groupId") String groupId, @PathVariable("status") String status) {
        dynamicGroupService.updateButton(groupId, status);
        return R.success();
    }

    @PostMapping("/{groupId}/heartbeat/{userId}")
    @Operation(summary = "心跳检测", description = "前端定时调用，更新心跳时间戳")
    public R<Void> heartbeat(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable String groupId,
        @Parameter(description = "用户ID", in = ParameterIn.PATH, required = true) @PathVariable String userId) {
        // 更新心跳时间戳
        dynamicGroupService.updateHeartbeat(groupId, userId);
        return R.success();
    }

}