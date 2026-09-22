package com.tdtech.cloudcmd.im.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.GroupRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.PoliceTicketRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCOV2;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketStatisticsCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;
import com.tdtech.cloudcmd.im.openapi.aop.OAuthContext;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.OpenApiPoliceTicketQO;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "警单相关接口", description = "警单相关接口")
@RestController
@RequestMapping("/openapi/v1/polticket")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.POLICE_TICKET)
@RequestLimit(business = "警单管理")
@Licensed(module = LicenseEnum.GROUP_COLLABORATION)
public class PolticketController {

    private static final Logger logger = LoggerFactory.getLogger(PolticketController.class);

    @DubboReference
    private PoliceTicketRpcApi policeTicketRpcApi;
    @DubboReference
    private GroupRpcApi groupRpcApi;

    @Resource
    private StreamBridge streamBridge;

    /**
     * 分页查询 PoliceTicket
     *
     * @param policeTicket 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询警单", description = "根据条件分页查询警单列表")
    @Parameter(name = "current", description = "当前页码")
    @Parameter(name = "size", description = "每页条数")
    @Parameter(name = "groupId", description = "群组ID，跟postId条件互斥")
    @Parameter(name = "bindFlag", description = "是否只看绑定的，groupId有值时才生效，默认全部")
    @Parameter(name = "postId", description = "协同岗ID，查协同岗关联的警单，跟groupId条件互斥")
    @Parameter(name = "startTime", description = "时间范围查询起始时间")
    @Parameter(name = "endTime", description = "时间范围查询结束时间")
    @Parameter(name = "name", description = "名称,模糊搜索")
    @Parameter(name = "code", description = "单号,模糊搜索")
    @Parameter(name = "content", description = "内容,模糊搜索")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Page<PoliceTicketVO>> findPage(@RequestParam(name = "current") Integer current,
        @RequestParam(name = "size") Integer size, OpenApiPoliceTicketQO policeTicket) {
        var r = policeTicketRpcApi.findPage(current, size, BeanCopyUtils.copyBean(policeTicket, PoliceTicketQO::new));
        return R.success(r);
    }

    @PostMapping(value = "/report", consumes = "application/json")
    @Operation(summary = "上报警单")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Void> report(@Parameter(description = "警单json", required = true) @RequestBody String body) {
        var token = OAuthContext.getToken();
        streamBridge.send("cloudcmd-im-jingxin-polticket", Map.of("systemCode", token.getClientId(), "param", body));
        return R.success();
    }

    @PostMapping("/groupbind/{groupId}")
    @Operation(summary = "警单关联，修改也是这个接口，每次传全量警单ID", description = "警单关联")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Void> bindGroup(@PathVariable("groupId") @NotNull Long groupId,
        @Parameter(description = "警单ID列表", required = true) @RequestBody @NotNull List<Long> ticketIds) {
        policeTicketRpcApi.bindGroup(groupId, ticketIds);
        return R.success();
    }

    @Operation(summary = "获取协同岗绑定类型")
    @Parameter(name = "postId", description = "协同岗ID", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "协同岗ID列表")
    @GetMapping("/tag/{postId}")
    public R<List<String>> getTag(@PathVariable("postId") Long postId) {
        return R.success(policeTicketRpcApi.getTag(postId));
    }

    @Operation(summary = "通过类型获取绑定协同岗")
    @Parameter(name = "tag", description = "类型", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "类型列表")
    @GetMapping("/post/{tag}")
    public R<List<Long>> getPosts(@PathVariable("tag") String tag) {
        return R.success(policeTicketRpcApi.getPosts(tag));
    }

    @GetMapping("/groupbind/{ticketId}")
    @Operation(summary = "获取群组", description = "只有ID")
    @Parameter(name = "polTicketId", description = "警单ID", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "成功获取群组ID列表")
    public R<List<Long>> groupIds(@PathVariable("ticketId") Long ticketId) {
        var groupQO = new GroupQO();
        groupQO.setPolTicketId(ticketId);
        return R.success(groupRpcApi.listGroupIds(groupQO));
    }

    /**
     * 一键建群
     */
    @PostMapping("/create-group")
    @Operation(summary = "根据警单创建群组", description = "根据警单一键创建群组")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "群组ID")
    public R<Long> createGroup(@RequestBody OpenApiCreateGroupCO createGroupCO) {
        var clientUserId = RequestHeaderUtil.getClientUserId();
        return groupRpcApi.createGroup(createGroupCO, clientUserId);
    }

    /**
     * 一键建群
     */
    @PostMapping("/create-group-by-idcard")
    @Operation(summary = "根据警单创建群组", description = "根据警单一键创建群组")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "群组ID")
    public R<Long> createGroupbyIdCard(@RequestBody OpenApiCreateGroupCOV2 openApiCreateGroupCOV2) {
        return groupRpcApi.createGroupV2(openApiCreateGroupCOV2);
    }

    /**
     * 根据部门IDS数组查询关联统计数据
     */
    @PostMapping("/coop-group")
    @Operation(summary = "根据部门IDS数组查询关联统计数据", description = "根据部门IDS数组查询关联统计数据")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "统计数据")
    public R<List<PoliceTicketStatisticsCO>> count(@RequestBody @NotNull List<Long> orgIds) {
        // TIPS: 获取请求头中的X-User-Id，暂时没用，SE说后续可能会用，暂时不用管
        var clientUserId = RequestHeaderUtil.getClientUserId();
        logger.info("count, orgIds: {}, clientUserId: {}", orgIds, clientUserId);
        return R.success(groupRpcApi.getPoliceTicketStatistics(orgIds));
    }
}
