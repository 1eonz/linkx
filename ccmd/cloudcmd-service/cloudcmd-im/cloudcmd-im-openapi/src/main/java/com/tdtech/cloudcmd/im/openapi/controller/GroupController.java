package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.GroupRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupMemberVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.LabelVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberResultVO;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCOV3;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.GroupOpenApiVO;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "群组管理", description = "群组相关接口")
@RestController
@RequestMapping("/openapi/v1/group")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.GROUP)
@RequestLimit(business = "协同岗管理")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    @DubboReference
    private GroupRpcApi groupRpcApi;

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    /** IM 错误码 → OpenAPI 错误码映射 */
    private static final Map<Integer, Integer> IM_CODE_MAPPING = Map.ofEntries(
            Map.entry(1080012, 5),  // 被邀请成员已在群组中
            Map.entry(1080011, 6),  // 群组不能添加三方用户
            Map.entry(1080004, 7),  // 群组最多不能超过N个成员
            Map.entry(1080001, 8),  // 没有修改群组的权限
            Map.entry(1080013, 10),  // 群组不存在或已解散
            Map.entry(204, 6)
    );

    @Operation(summary = "分页查询群组", description = "分页获取群组列表，支持按条件筛选")
    @ApiResponse(responseCode = "200", description = "成功返回群组列表",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/page")
    public R<CcmdPage<GroupOpenApiVO>> listGroups(CcmdPageParam ccmdPageParam) {
        var page = groupRpcApi.listGroupPaged(ccmdPageParam);
        if (page.isEmpty()) {
            return R.success(page.mult(Collections.emptyList()));
        }
        var records = BeanCopyUtils.copyList(page.getRecords(), GroupOpenApiVO::new);
        var gids = records.stream().map(GroupOpenApiVO::getGroupId).collect(Collectors.toList());
        var groupTagVOS = groupRpcApi.listTagsByGroups(gids);
        if (groupTagVOS == null || groupTagVOS.isEmpty()) {
            return R.success(page.mult(records));
        }
        var tagMap = groupTagVOS.stream().collect(Collectors.groupingBy(GroupTagVO::getGroupId, Collectors.toList()));
        for (var record : records) {
            if (tagMap.containsKey(record.getGroupId())) {
                record.setTags(tagMap.get(record.getGroupId()));
            }
        }
        return R.success(page.mult(records));
    }

    @Operation(summary = "查询群组成员", description = "获取指定群组的成员列表")
    @Parameter(name = "groupId", description = "群组ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功返回群组成员列表",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/{groupId}/members")
    public R<CcmdPage<GroupMemberVO>> groupMembers(@PathVariable("groupId") Long groupId, CcmdPageParam pageParam) {
        var page = groupRpcApi.getArchiveMemberPage(groupId, pageParam);
        return R.success(page);
    }

    @Operation(summary = "更新群组成员", description = "为指定群组添加或删除成员")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "X-User-Id", description = "调用人员的警信用户ID", in = ParameterIn.HEADER)
    @Parameter(name = "groupId", description = "群组ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功更新群组成员",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @PutMapping("/{groupId}/member")
    public R<UpdateGroupMemberResultVO> updateGroupMember(
            @PathVariable("groupId") Long groupId,
            @RequestBody @Valid UpdateGroupMemberCO co,
            @RequestHeader(value = "X-User-Id") String xUserId) {

        if (!RequestHeaderUtil.validateXUserId(xUserId, imUserRpcApi)) {
            return R.failure(2, "X-User-Id无效");
        }

        // 校验 userIds 和 idCards 二选一必传
        boolean hasUserIds = StringUtils.isNotBlank(co.getUserIds());
        boolean hasIdCards = StringUtils.isNotBlank(co.getIdCards());
        if (hasUserIds == hasIdCards) {
            return R.failure(3, "userIds和idCards二选一必填，且不支持混填");
        }

        if (co.getOpType() == null || (co.getOpType() != 1 && co.getOpType() != 2)) {
            return R.failure(4, "opType不合法，必须为1或2");
        }

        if (co.getJoinType() == null || (co.getJoinType() != 1 && co.getJoinType() != 3)) {
            return R.failure(9, "joinType不合法，必须为1或3");
        }

        // 校验传入的 idCard 是否为真实用户
        String queryIdCards = hasIdCards ? co.getIdCards().replace(";", ",") : null;
        if (hasIdCards) {
            var userResult = imUserRpcApi.getUsersInfo(null, queryIdCards, xUserId);
            if (userResult == null) {
                return R.failure(-1, "未知错误");
            }
            // 当 results 为空时，检查 failures 是否有不存在的用户
            if (userResult.getResults() == null || userResult.getResults().isEmpty()) {
                if (userResult.getFailures() != null && !userResult.getFailures().isEmpty()) {
                    List<String> failedIds = new ArrayList<>();
                    for (var fail : userResult.getFailures()) {
                        if (StringUtils.isNotBlank(fail.getIdCard())) {
                            failedIds.add(fail.getIdCard());
                        }
                    }
                    if (!failedIds.isEmpty()) {
                        return R.failure(6, "群组不能添加/删除三方用户:" + String.join(";", failedIds));
                    }
                }
                return R.failure(-1, "未知错误");
            }
            Set<String> found = new HashSet<>();
            for (var user : userResult.getResults()) {
                if (user.getId() != null) {
                    found.add(String.valueOf(user.getId()));
                }
                if (StringUtils.isNotBlank(user.getIdCard())) {
                    found.add(user.getIdCard());
                }
            }
            List<String> notFounds = new ArrayList<>();

            for (String s : co.getIdCards().split(";")) {
                if (StringUtils.isNotBlank(s.trim()) && !found.contains(s.trim())) {
                    notFounds.add(s.trim());
                }
            }
            if (!notFounds.isEmpty()) {
                return R.failure(6, "群组不能添加三方用户:" + String.join(";", notFounds));
            }
        }


        long operatorUserId = Long.parseLong(xUserId);
        UpdateGroupMemberResultVO result = groupRpcApi.updateGroupMember(groupId, co, operatorUserId);
        if (result.getCode() != null && result.getCode() != 0) {
            int openApiCode = IM_CODE_MAPPING.getOrDefault(result.getCode(), result.getCode());
            logger.info("openapi: '/openapi/v1/group/{}/member', X-User-Id: {}, imCode: {}, msg: {}", groupId, xUserId, result.getCode(), result.getMsg());
            return R.failure(openApiCode, result.getMsg());
        }
        logger.info("openapi: '/openapi/v1/group/{}/member', X-User-Id: {}, msg: {}", groupId , xUserId, result.getMemberResults());
        return R.success();
    }

    /**
     * 关注群组
     */
    @SneakyThrows
    @Operation(summary = "关注群组", description = "用户关注指定群组")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "groupId", description = "群组ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功关注群组",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @PostMapping("/care")
    public R<Void> careGroup(@RequestParam Long userId, @RequestParam Long groupId) {
        groupRpcApi.careGroup(userId, groupId);
        return R.success();
    }

    /**
     * 取消关注群组
     */
    @SneakyThrows
    @Operation(summary = "取消关注群组", description = "用户取消关注指定群组")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "groupId", description = "群组ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功取消关注群组",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @DeleteMapping("/uncare")
    public R<Void> uncareGroup(@RequestParam Long userId, @RequestParam Long groupId) {
        groupRpcApi.uncareGroup(userId, groupId);
        return R.success();
    }

    /**
     * 批量取消关注群组
     */
    @SneakyThrows
    @Operation(summary = "批量取消关注群组", description = "用户批量取消关注多个群组")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "groupIdStr", description = "群组ID字符串，以逗号分隔", required = true)
    @ApiResponse(responseCode = "200", description = "成功批量取消关注群组",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @DeleteMapping("/uncare/batch")
    public R<Void> batchUncareGroups(@RequestParam Long userId, @RequestParam String groupIdStr) {
        var split = groupIdStr.split(",");
        groupRpcApi.batchUncareGroups(userId, Arrays.stream(split).map(Long::parseLong).collect(Collectors.toList()));
        return R.success();
    }

    /**
     * 分页查询用户关注的群组
     */
    @SneakyThrows
    @Operation(summary = "分页查询用户关注的群组", description = "获取指定用户关注的群组列表，支持分页查询")
    @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.PATH)
    @ApiResponse(responseCode = "200", description = "成功返回用户关注的群组列表",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/care/{userId}/page")
    public R<PageResult<Long>> getCaredGroupsPage(@PathVariable Long userId, @NotNull @Valid CcmdPageParam pageParam) {
        try {
            return R.success(groupRpcApi.getCaredGroupsPage(userId, pageParam));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 返回当前协同群总数
     */
    @Operation(summary = "获取当前协同群总数", description = "返回当前协同群的总数")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/create/count")
    public R<List<GroupCreationCountVO>> groupCreateCount(
        @Parameter(name = "departmentCode", description = "部门编码,逗号分割", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime,
        @Parameter(name = "source", description = "来源，0 其他 1 一键建群 2 警单建群",
            in = ParameterIn.QUERY) @RequestParam(value = "source", required = false) Integer source) {
        return R.success(groupRpcApi.groupCreateCount(departmentCode, startTime, endTime, source));
    }

    /**
     * 返回所有协同群总数
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @Operation(summary = "获取所有协同群总数", description = "返回所有协同群的总数")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/create/all")
    public R<Integer> groupCreateAll(
        @Parameter(name = "departmentCode", description = "部门编码,逗号分割", in = ParameterIn.QUERY) @RequestParam(
            name = "departmentCode", required = false) String departmentCode,
        @Parameter(name = "startTime", description = "开始时间", in = ParameterIn.QUERY) @RequestParam(
            name = "startTime", required = false) String startTime,
        @Parameter(name = "endTime", description = "结束时间", in = ParameterIn.QUERY) @RequestParam(name = "endTime",
            required = false) String endTime,
        @Parameter(name = "source", description = "来源，0 其他 1 一键建群 2 警单建群",
            in = ParameterIn.QUERY) @RequestParam(value = "source", required = false) Integer source) {
        return R.success(groupRpcApi.groupCreateAll(departmentCode, startTime, endTime, source));
    }

    /**
     * 获取指定用户的群组列表（未归档+已归档）
     */
    @Operation(summary = "获取指定用户的群组列表", description = "根据用户ID或身份证号获取群组列表（含未归档和已归档）")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "X-User-Id", description = "调用人的警信用户ID", in = ParameterIn.HEADER)
    @GetMapping("/{id}")
    public R<CcmdPage<OpenApiGroupVO>> getUserGroups(
            @Parameter(description = "警信用户ID或身份证号", required = true)
            @PathVariable("id") String id,
            @Parameter(description = "0:警信用户ID;1:身份证号码", required = true)
            @RequestParam(value = "idType") Integer idType,
            @Parameter(description = "0:全部;1:普通群组;2:协同群组")
            @RequestParam(value = "groupType", required = false, defaultValue = "2") Integer groupType,
            @Parameter(description = "0:全部;1:一键建群;5:一键调度;4:职能建群;3:自定义建群")
            @RequestParam(value = "createType", required = false, defaultValue = "1") Integer createType,
            @Parameter(description = "1:我创建的;3:我可查看的但不是成员;4:我是成员;5:我的所有(default)")
            @RequestParam(value = "scope", required = false, defaultValue = "5") Integer scope,
            @Parameter(description = "分页页码，默认1")
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @Parameter(description = "分页每页的条目数，默认10")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestHeader(value = "X-User-Id") String xUserId) {

        if (!RequestHeaderUtil.validateXUserId(xUserId, imUserRpcApi)) {
            return R.failure(2, "X-User-Id无效");
        }

        if (StringUtils.isBlank(id)) {
            return R.failure(3, "id无效或找不到指定用户");
        }

        if (idType == null || (idType != 0 && idType != 1)) {
            return R.failure(4, "idType无效，必须为0或1");
        }

        if (groupType != null && groupType != 0 && groupType != 1 && groupType != 2) {
            return R.failure(5, "groupType无效，必须为0、1或2");
        }

        if (createType != null && createType != 0 && createType != 1 && createType != 3
                && createType != 4 && createType != 5) {
            return R.failure(6, "createType无效，必须为0、1、3、4或5");
        }

        if (scope != null && scope != 1 && scope != 3 && scope != 4 && scope != 5) {
            return R.failure(7, "scope无效，必须为1、3、4或5");
        }

        Long userId;
        if (idType != null && idType == 1) {
            var result = imUserRpcApi.getUsersInfo(null, id, xUserId);
            if (result == null || result.getResults() == null || result.getResults().isEmpty()) {
                return R.failure(I18nUtil.get("ResponseCodeEnum_COMMON_ERROR_172"));
            }
            userId = result.getResults().get(0).getId();
        } else {
            try {
                userId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                return R.failure("id格式错误，必须为数字");
            }
        }

        OpenApiGroupQO qo = new OpenApiGroupQO();
        qo.setUserId(userId);
        qo.setGroupType(groupType);
        qo.setCreateType(createType);
        qo.setScope(scope);
        qo.setPageNum(page);
        qo.setPageSize(pageSize);

        logger.info("openapi: '/openapi/v1/group/{}', X-User-Id: {}, msg: success", id , xUserId);
        return R.success(groupRpcApi.listGroupsByUser(qo));
    }

    @Operation(summary = "获取指定用户的群组计数", description = "根据用户ID或身份证号获取群组计数")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "X-User-Id", description = "调用人的警信用户ID", in = ParameterIn.HEADER)
    @GetMapping("/{id}/count")
    public R<OpenApiGroupCountVO> getGroupCountByUserId(
            @Parameter(description = "警信用户ID或身份证号", required = true)
            @PathVariable("id") String id,
            @Parameter(description = "0:警信用户ID;1:身份证号码", required = true)
            @RequestParam(value = "idType") Integer idType,
            @RequestHeader(value = "X-User-Id") String xUserId) {

        if (!RequestHeaderUtil.validateXUserId(xUserId, imUserRpcApi)) {
            return R.failure(2, "X-User-Id无效");
        }

        if (idType == null || (idType != 0 && idType != 1)) {
            return R.failure(3, "idType无效，必须为0或1");
        }

        Long userId;
        if (idType == 1) {
            var result = imUserRpcApi.getUsersInfo(null, id, xUserId);
            if (result == null || result.getResults() == null || result.getResults().isEmpty()) {
                return R.failure(4, "id无效或找不到指定用户");
            }
            userId = result.getResults().get(0).getId();
        } else {
            try {
                userId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                return R.failure(4, "id无效或找不到指定用户");
            }
        }
        logger.info("openapi: '/openapi/v1/group/{}/count', X-User-Id: {}, msg: success", id , xUserId);
        return R.success(groupRpcApi.getGroupCountByUserId(userId));
    }

    /**
     * 查询所有标签
     */
    @SneakyThrows
    @Operation(summary = "查询所有标签", description = "根据名称模糊查询所有标签")
    @Parameter(name = "name", description = "标签名称（可选）")
    @ApiResponse(responseCode = "200", description = "成功返回用户关注的群组列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/tags")
    public R<List<LabelVO>> getCaredGroupsPage(
            @Parameter(description = "标签名称（可选）") @RequestParam(required = false) String name,
            @Parameter(description = "标签作用域") @RequestParam(required = false) Integer scope,
            @Parameter(description = "返回层级，0 全部，1 一级") @RequestParam(required = false, defaultValue = "0") int level) {
        try {
            // TIPS: 获取请求头中的X-User-Id，暂时没用，SE说后续可能会用，暂时不用管
            var clientUserId = RequestHeaderUtil.getClientUserId();
            logger.info("getAllLabels, name: {}, scope: {}, level: {}, clientUserId: {}", name, scope, level, clientUserId);
            return R.success(groupRpcApi.listAllLabels(name, scope, level));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建群组", description = "根据身份证号或用户ID一键创建群组")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "群组ID")
    public R<Long> createGroupByIdCardsAndUserIds(@RequestBody @Valid OpenApiCreateGroupCOV3 openApiCreateGroupCOV3) {
        var clientUserId = RequestHeaderUtil.getClientUserId();
        var clientIdCard = RequestHeaderUtil.getClientIdCard();
        return groupRpcApi.createGroupByIdCardsAndUserIds(openApiCreateGroupCOV3, clientUserId, clientIdCard);
    }

}
