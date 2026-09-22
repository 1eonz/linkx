package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SendApproveCardCO;
import com.tdtech.cloudcmd.im.jingxin.server.config.AvatarScheduler;
import com.tdtech.cloudcmd.im.jingxin.server.config.PostSupportScheduler;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImCommonService;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * @author: S063874
 * @date: 2026-01-14 15:32
 */
@Tag(name = "im接口对接通接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/im")
@RequiredArgsConstructor
public class ImCommonController {

    private final ImCommonService imCommonService;

    private final PostSupportScheduler postSupportScheduler;

    private final AvatarScheduler avatarScheduler;


    @GetMapping("/queryUser")
    @Operation(summary = "查询用户", description = "根据条件查询用户信息")
    public R queryUser(@Parameter(description = "组织代码") @RequestParam(name = "code",required = false) String code,
                       @Parameter(description = "是否包含子级") @RequestParam(name = "includeChildren",
                               required = false) Integer includeChildren,
                       @Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
                       @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
                       @Parameter(description = "关键字") @RequestParam(name = "keywords", required = false) String keywords,
                       @Parameter(description = "部门id") @RequestParam(name = "deptId", required = false) String deptId,
                       @Parameter(description = "用户名") @RequestParam(name = "name", required = false) String name
                       ) {
        return R.success(imCommonService.queryUser(code, includeChildren, keywords, deptId, name, pageNum, pageSize));
    }

    @GetMapping("/query/group/type")
    @Operation(summary = "根据用户类型查询所在群组", description = "根据条件查询用户信息")
    public R queryGroupByUserType(@Parameter(description = "用户类型(1:普通用户,2:智能体用户)", required = true) @RequestParam("type") String type,
                                  @Parameter(description = "用户id(类型为普通用户id时传入),智能体用户时传入智能体用户类型") @RequestParam(name = "key") String key,
                                  @Parameter(description = "keywords") @RequestParam(name = "keywords", required = false) String keywords,
                                  @Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
                                  @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize
    ) {
        return R.success(imCommonService.queryGroupByUserType(type, key, keywords, pageNum, pageSize));
    }

    @GetMapping("/users/{userId}/page")
    @Operation(summary = "分页获取指定用户的好友/关注列表openApi", description = "分页获取指定用户的好友/关注列表openApi")
    public R getFriendsOrFollows(@PathVariable String userId,
                                 @Parameter(description = "好友/关注列表类型(0-好友 1-关注 2-所有，默认值0)") @RequestParam("userType") Integer userType,
                                 @Parameter(description = "页码", required = true) @RequestParam("pageNo") Integer pageNo,
                                 @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize) {
        return R.success(imCommonService.getFriendsOrFollows(userId, userType, pageNo, pageSize));
    }

    @GetMapping("/users/tree")
    @Operation(summary = "指定用户的通讯录树形展示openApi", description = "指定用户的通讯录树形展示openApi")
    public R getUserTree(@Parameter(description = "用户id") @RequestParam(name = "userId", required = false) String userId,
                         @Parameter(description = "部门id") @RequestParam(name = "departmentId", required = false) String departmentId) {
        return R.success(imCommonService.getUserTree(userId, departmentId));
    }

    @GetMapping("/users/{userId}/departments")
    @Operation(summary = "指定用户的通讯录树形展示openApi", description = "指定用户的通讯录树形展示openApi")
    public R getUserDepartments(@PathVariable String userId,
                                @Parameter(description = "部门id") @RequestParam(name = "departmentId", required = false) String departmentId,
                                @Parameter(description = "页码", required = true) @RequestParam("pageNo") Integer pageNo,
                                @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize) {
        return R.success(imCommonService.getDepartmentPage(userId, departmentId, pageNo, pageSize));
    }

    @GetMapping("/users/page")
    @Operation(summary = "分页查询人员列表信息", description = "分页查询人员列表信息")
    public R getUserPage(@Parameter(description = "用户名/电话") @RequestParam(name = "keywords", required = false) String keywords,
                         @Parameter(description = "页码", required = true) @RequestParam("pageNo") Integer pageNo,
                         @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
                         @Parameter(description = "用户名") @RequestParam(name = "name", required = false) String name,
                         @Parameter(description = "组织机构id") @RequestParam(name = "departmentId", required = false) String departmentId) {
        return R.success(imCommonService.getUsersPage(name, keywords, departmentId, pageNo, pageSize));
    }

    @PostMapping("/group/create")
    @Operation(summary = "创建群组", description = "创建群组")
    @LogReport(type = OperationTypeEnum.COLLABORATION_GROUP_INSERT)
    public R createGroup(@Parameter(description = "建群参数", required = true) @Validated @RequestBody @LogReportParam(field = "departmentName") CreateGroupCO createGroupVO) {
        return R.success(imCommonService.createGroup(createGroupVO));
    }

    @PostMapping("/functionaldepts/group/create")
    @Operation(summary = "根据职能部门建群创建群组", description = "根据职能部门建群创建群组")
    @LogReport(type = OperationTypeEnum.COLLABORATION_GROUP_INSERT)
    public R createGroupByFunctionalDepartment(@Parameter(description = "建群参数", required = true)
                                                   @Validated @RequestBody @LogReportParam(field = "departmentName")
                                                   CreateGroupCO createGroupVO) {
        return R.success(imCommonService.createGroupByFunctionalDepartment(createGroupVO));
    }

    @PostMapping("/pull/history/group")
    public R pullHistoryGroup(@RequestParam String sync) {
        CompletableFuture.runAsync(() -> {
            try {
                imCommonService.pullHistoryGroup(sync);
                log.info("异步拉取历史群组操作完成");
            } catch (Exception e) {
                log.error("异步拉取历史群组操作失败", e);
            }
        });
        return R.success(true);
    }

    @PostMapping("/sync/group/post")
    public R syncGroupPost() {
        postSupportScheduler.scheduledSyncPost();
        return R.success(true);
    }

    @PostMapping("/sync/avatar")
    @Operation(summary = "手动触发头像下载任务", description = "手动触发头像增量下载任务")
    public R syncAvatar() {
            try {
                avatarScheduler.downloadAvatars();
                log.info("头像下载任务执行完成");
            } catch (Exception e) {
                log.error("头像下载任务执行失败", e);
            }
        return R.success(true);
    }

    @PostMapping("/send/approve/card")
    @Operation(summary = "发送审批卡片消息", description = "发送审批卡片消息")
    public R sendApproveCard(@Parameter(description = "发送审批卡片消息参数", required = true) @Validated @RequestBody SendApproveCardCO sendApproveCardCO) {
        return R.success(imCommonService.sendApproveCard(sendApproveCardCO));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "根据userId查询用户信息", description = "根据userId查询IM用户信息")
    public R<ImUser> getUserByUserId(@Parameter(description = "用户ID", required = true) @PathVariable String userId) {
        return R.success(imCommonService.getUserByUserId(userId));
    }

    @GetMapping("/users/idCard/{idCard}")
    @Operation(summary = "根据idCard查询用户信息", description = "根据idCard查询IM用户信息")
    public R<ImUser> getUserByUserIdCard(@Parameter(description = "用户身份证号码", required = true) @PathVariable String idCard) {
        return R.success(imCommonService.getUserByUserIdCard(idCard));
    }
}