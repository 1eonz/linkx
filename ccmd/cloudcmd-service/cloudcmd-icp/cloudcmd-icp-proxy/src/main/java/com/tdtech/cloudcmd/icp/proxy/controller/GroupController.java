package com.tdtech.cloudcmd.icp.proxy.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.GroupMemberVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.GroupVO;
import com.tdtech.cloudcmd.icp.proxy.service.GroupMemberService;
import com.tdtech.cloudcmd.icp.proxy.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "ICP群组管理", description = "ICP群组相关接口")
@RestController
@RequestMapping("/proxy/icp/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;

    @PostMapping
    @Operation(summary = "创建群组", description = "在ICP上创建群组并同步到本地数据库")
    public R<Void> create(
        @Parameter(description = "群组信息", required = true) @Valid @RequestBody GroupVO groupVO) {
        groupService.create(groupVO);
        return R.success();
    }

    @PostMapping("/{groupId}/member")
    @Operation(summary = "添加群组成员", description = "向指定群组添加成员")
    public R<Void> addMember(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable Long groupId,
        @Parameter(description = "群组成员信息", required = true) @Valid @RequestBody GroupMemberVO groupMemberVO) {
        groupMemberService.addMember(groupMemberVO);
        return R.success();
    }

    @DeleteMapping("/{groupId}/member")
    @Operation(summary = "删除群组成员", description = "从指定群组删除成员")
    public R<Void> deleteMember(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable Long groupId,
        @Parameter(description = "成员isdn列表，多个以逗号分隔", required = true) @RequestParam String isdns) {
        groupMemberService.deleteMember(groupId, isdns);
        return R.success();
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "删除群组", description = "删除指定群组及其所有成员")
    public R<Void> delete(
        @Parameter(description = "群组ID", in = ParameterIn.PATH, required = true) @PathVariable Long groupId) {
        groupService.delete(groupId);
        return R.success();
    }
}
