package com.tdtech.cloudcmd.im.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.CollaborationRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.*;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.CollaborationPostListVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTasksCountVO;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "协同岗管理", description = "协同岗相关接口")
@RestController
@RequestMapping("/openapi/v1/collaboration")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.COLLABORATIVE_STATISTICS)
@RequestLimit(business = "协同岗管理")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class CollaborationController {

    private static final String LIST_ZERO_ON_DUTY_POSTS_CACHE_KEY = "cloudcmd:openapi:listZeroOnDutyPosts:cache:";

    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    @DubboReference
    private CollaborationRpcApi collaborationRpcApi;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ImHttpClient imHttpClient;

    /**
     * 获取协同岗在线情况
     */
    @Operation(summary = "获取协同岗在线情况", description = "统计指定时间范围内协同岗在线人员情况")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/count")
    public R<CollaborationStatisticsVO> count(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        return R.success(collaborationRpcApi.count(departmentCode, startTime, endTime));
    }

    /**
     * 获取0人员在线的协同岗列表
     */
    @Operation(summary = "获取0人员在线的协同岗列表", description = "获取指定时间范围内无人员在线的协同岗列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/listZeroOnDutyPosts")
    public R<List<CollaborationAttendance>> listZeroOnDutyPosts(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode) {
        var key = LIST_ZERO_ON_DUTY_POSTS_CACHE_KEY + (departmentCode == null ? "" : departmentCode);
        var data = redisUtil.get(key, new TypeReference<List<CollaborationAttendance>>() {
        });
        if (data != null) {
            return R.success(data);
        }
        data = collaborationRpcApi.listZeroOnDutyPosts(departmentCode);
        redisUtil.set(key, data, Duration.ofMinutes(1L));
        return R.success(data);
    }

    /**
     * 返回协同岗对应的所有群组id,参数支持批量, 协同岗标签 List<协同岗id，去查询群组id> Map<协同岗id, 群组>
     */
    @Operation(summary = "获取协同岗对应的群组ID", description = "返回协同岗对应的所有群组ID，支持批量查询")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/getGroupIds")
    public R<Map<Long, List<Long>>> getGroupIds(
        @Parameter(name = "collaborationIds", description = "协同岗ID列表", in = ParameterIn.QUERY) @RequestParam(
            name = "collaborationIds") List<Long> collaborationIds,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        return R.success(collaborationRpcApi.getGroupIds(collaborationIds, startTime, endTime));
    }

    /**
     * 返回协同岗和被@总次数，支持批量查找分页 传入 List<协同岗id，去查询用户id> task 表去查询次数
     */
    @Operation(summary = "获取协同岗被@总次数", description = "返回协同岗和被@总次数，支持批量查找分页")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/getCollabCount")
    public R<List<CollaborationTaskCountResponse>> getCollabCount(
        @Parameter(name = "collaborationIds", description = "协同岗ID列表", in = ParameterIn.QUERY) @RequestParam(
            name = "collaborationIds", required = false) List<Long> collaborationIds,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        return R.success(collaborationRpcApi.getCollabCount(collaborationIds, startTime, endTime));
    }

    /**
     * 协同岗处理问题平均时长统计
     */
    @Operation(summary = "协同岗处理问题平均时长统计", description = "统计协同岗处理问题的平均时长")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/replyDuration")
    public R<List<CollaborationReplyDurationVO>> replyDuration(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        return R.success(collaborationRpcApi.replyDuration(departmentCode, startTime, endTime));
    }

    @Operation(summary = "处置回复时长列表", description = "获取协同岗处置回复时长列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/disposition/replyDuration/list")
    public R<List<CollaborationDispositionVO>> dispositionReplyDurationList(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        return R.success(collaborationRpcApi.dispositionReplyDurationList(departmentCode, startTime, endTime));
    }

    /**
     * 回复统计top10
     */
    @Operation(summary = "处置回复统计", description = "处置回复统计")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/replyCount")
    public R<List<CollaborationReplyTotalVO>> replyCount(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {

        return R.success(collaborationRpcApi.replyCount(departmentCode, startTime, endTime));
    }

    /**
     * 协同岗在线统计列表
     */
    @Operation(summary = "协同岗在线统计列表", description = "协同岗在线统计列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/online/list")
    public R<List<CollaborationPostOnlineVO>> onlineStatistics(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {

        return R.success(collaborationRpcApi.onlineStatistics(departmentCode, startTime, endTime));
    }

    @Operation(summary = "协同岗在线时长统计列表", description = "协同岗在线时长统计列表")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/online/duration")
    public R<CcmdPage<CollaborationPostOnlineDurationVO>> onlineDuration(CcmdPageParam pageParam,
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) @DateTimeFormat(
            pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(collaborationRpcApi.onlineDuration(pageParam, startTime, endTime, departmentCode));
    }

    /**
     * 统计逾期的消息数据
     * <p>
     * task表有直接查询
     */
    @Operation(summary = "统计逾期消息数据", description = "统计逾期的消息数据，从task表直接查询")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/overdue")
    public R<Page<CollaborationOverdueVO>> getOverdueList(
        @Parameter(name = "departmentCode", description = "部门编码", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime,
        @Parameter(name = "pageSize", description = "页面条数", in = ParameterIn.QUERY) @RequestParam(name = "pageSize",
            required = false) Integer pageSize,
        @Parameter(name = "pageNum", description = "页码", in = ParameterIn.QUERY) @RequestParam(name = "pageNum",
            required = false) Integer pageNum) {
        Page<CollaborationOverdueVO> data =
            collaborationRpcApi.getOverdueList(departmentCode, startTime, endTime, pageSize, pageNum);
        return R.success(data);
    }

    @Operation(summary = "获取协同岗考勤分页数据", description = "分页获取协同岗考勤数据")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/attendance/page")
    public R<Page<CollaborationAttendance>> getPage(
        @Parameter(name = "pageNum", description = "页码", in = ParameterIn.QUERY) @RequestParam(
            name = "pageNum") int pageNum,
        @Parameter(name = "pageSize", description = "每页数量", in = ParameterIn.QUERY) @RequestParam(
            name = "pageSize") int pageSize,
        @Parameter(name = "postName", description = "岗位名称", in = ParameterIn.QUERY) @RequestParam(name = "postName",
            required = false) String postName,
        @Parameter(name = "orgName", description = "组织名称", in = ParameterIn.QUERY) @RequestParam(name = "orgName",
            required = false) String orgName,
        @Parameter(name = "orgId", description = "组织ID", in = ParameterIn.QUERY) @RequestParam(name = "orgId",
            required = false) Long orgId,
        @Parameter(name = "personName", description = "人员姓名", in = ParameterIn.QUERY) @RequestParam(
            name = "personName", required = false) String personName,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime) {
        Page<CollaborationAttendance> page =
            collaborationRpcApi.getPage(pageNum, pageSize, postName, orgName, orgId, personName, startTime, endTime);
        return R.success(page);
    }

    @Operation(summary = "获取协同岗分页数据", description = "分页获取协同岗数据")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/list")
    public R<CcmdPage<CollaborationPostListVO>> listPostPaged(@NotNull @Valid CcmdPageParam pageParam,
        @RequestParam(value = "departmentCode", required = false) String departmentCode,
        @RequestParam(value = "groupId", required = false) Long groupId) {
        var page = collaborationRpcApi.postPage(pageParam, departmentCode, groupId);
        if (page.isEmpty()) {
            return R.success(CcmdPage.empty(pageParam));
        }
        var mult = page.mult(CollaborationPostListVO::new);
        var uids = mult.getRecords().stream().map(CollaborationPostVO::getRelatedUserIds)
            .filter(a -> a != null && !a.isBlank()).map(a -> a.split(",")).flatMap(Arrays::stream)
            .filter(a -> !a.isBlank()).distinct().collect(Collectors.toList());
        var group = CollectionUtils.group(uids, 50);
        List<ImUser> users = new LinkedList<>();
        for (var entry : group.entrySet()) {
            var ul = imHttpClient.userPage(null, String.join(",", entry.getValue()));
            users.addAll(ul.getResults());
        }
        for (var record : mult.getRecords()) {
            var relatedUserIds = record.getRelatedUserIds();
            if (relatedUserIds == null || relatedUserIds.isBlank()) {
                continue;
            }
            var members = new LinkedList<ImUser>();
            record.setMembers(members);
            var split = relatedUserIds.split(",");
            for (var sp : split) {
                for (var user : users) {
                    if (Objects.equals(user.getId().toString(), sp)) {
                        members.add(user);
                    }
                }
            }
        }
        return R.success(mult);
    }

    @Operation(summary = "分页查询协同岗", description = "根据条件分页查询协同岗信息")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/{postId}")
    R<CollaborationPostVO> getCollaborationPostDetail(@PathVariable Long postId) {
        // TIPS: 获取请求头中的X-User-Id，暂时没用，SE说后续可能会用，暂时不用管
        var clientUserId = RequestHeaderUtil.getClientUserId();
        logger.info("getCollaborationPostDetail, postId: {}, clientUserId: {}", postId, clientUserId);
        return R.success(collaborationRpcApi.getCollaborationPostDetail(postId));
    }

    @Operation(summary = "获取指定协同岗的任务统计", description = "根据协同岗id查询协同岗任务统计")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/{collaborationId}/tasks/count")
    R<CollaborationTasksCountVO> getCollaborationTasksCount(@PathVariable Long collaborationId) {
        logger.info("getCollaborationTasksCount, collaborationId: {}", collaborationId);
        return R.success(collaborationRpcApi.getCollaborationTasksCount(collaborationId));
    }
}
