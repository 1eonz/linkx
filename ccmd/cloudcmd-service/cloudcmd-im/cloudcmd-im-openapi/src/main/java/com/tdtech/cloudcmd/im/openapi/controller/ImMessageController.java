package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.ImMessageRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.IMMsgRspVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.MMSMsgVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendImMessageCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendNotificationCO;
import com.tdtech.cloudcmd.im.openapi.aop.OAuthContext;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.im.openapi.service.notification.NotificationService;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tdtech.cloudcmd.im.openapi.utils.RequestHeaderUtil;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "IM消息接口", description = "发送IM卡片消息接口")
@Slf4j
@RestController
@RequestMapping("/openapi/v1")
@OpenApiOauth(BusinessScopeEnum.IM_MESSAGE)
@RequestLimit(business = "IM消息")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class ImMessageController {

    @DubboReference
    private ImMessageRpcApi imMessageRpcApi;

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    @Resource
    private NotificationService notificationService;

    @Operation(summary = "发送IM卡片消息", description = "发送IM消息，发送人为系统虚拟用户")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "X-User-Id", description = "接口调用人的警信ID", required = false, in = ParameterIn.HEADER)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回发送结果",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = R.class)))})
    @PostMapping("/im/msg")
    public R<IMMsgRspVo> sendImMessage(
            @Valid @RequestBody SendImMessageCO co,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-App-Id", required = false) String xAppId) {
        // X-User-Id 非必填：传了才校验有效性，不传跳过校验
        if (StringUtils.isNotBlank(xUserId) && !RequestHeaderUtil.validateXUserId(xUserId, imUserRpcApi)) {
            return R.failure(2, "X-User-Id无效");
        }
        // 透传 X-App-Id（虚拟用户appId），为空时由 RPC 层走默认虚拟用户发送逻辑
        co.setAppId(xAppId);

        // 从 token 获取三方应用 clientId
        Token token = OAuthContext.getToken();
        String clientId = token != null ? token.getClientId() : null;
        log.info("openapi: '/openapi/v1/im/msg' 入参, X-User-Id: {}, X-App-Id: {}, clientId: {}, co: {}",
                xUserId, xAppId, clientId, co);

        var result = imMessageRpcApi.sendImMessage(co);
        log.info("openapi: '/openapi/v1/im/msg', X-User-Id: {}, X-App-Id: {}, clientId: {}, msg: {}", xUserId, xAppId, clientId, result.getMsg());
        if (result.getCode() != 0) {
            return R.failure(result.getCode(), result.getMsg());
        }
        // 当存在不存在的目标用户等提示信息时，保留服务层返回的 msg
        if (StringUtils.isNotBlank(result.getMsg()) && !"成功".equals(result.getMsg())) {
            return R.success(result.getCode(), result.getMsg());
        }
        return R.success();
    }

    @Operation(summary = "构建彩信消息体", description = "根据文件ID构建彩信消息体，内部查询文件并上传警信换取fileKey")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回彩信消息体",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = R.class)))})
    @GetMapping("/im/mms/msg")
    public R<MMSMsgVo> buildMmsMsgVo(
            @Parameter(description = "文件ID（文件存储记录的fileMd5）", required = true)
            @RequestParam("fileId") String fileId) {
        try {
            MMSMsgVo mmsMsgVo = imMessageRpcApi.buildMmsMsgVo(fileId);
            log.info("openapi: '/openapi/v1/im/mms/msg', fileId: {}", fileId);
            return R.success(mmsMsgVo);
        } catch (Exception e) {
            log.error("openapi: '/openapi/v1/im/mms/msg' 构建彩信消息体失败, fileId: {}", fileId, e);
            return R.failure(e.getMessage());
        }
    }

    @Operation(summary = "发送任务通知", description = "发送任务通知消息，可达公安网的生活区。targetUserIds和targetIdCards二选一必填。")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "Content-Type", description = "内容类型", required = true, in = ParameterIn.HEADER)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回发送结果",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = R.class)))})
    @PostMapping("/im/notification")
    public R<IMMsgRspVo> sendNotification(
            @Valid @RequestBody SendNotificationCO co) {
        Token token = OAuthContext.getToken();
        if (token != null && token.getClientId() != null) {
            co.setAppId(token.getClientId());
        }
        // 先生成通知ID，随消息下发并作为持久化记录主键
        Long notificationId = IdWorker.getId();
        co.setNotificationId(notificationId);

        if (co.getShowInNotification() == null || (co.getShowInNotification() != 0 && co.getShowInNotification() != 1)) {
            return R.failure("showInNotification参数无效，必须为0或1");
        }

        var result = imMessageRpcApi.sendNotification(co);
        log.info("openapi: '/openapi/v1/im/notification', msg: {}", result.getMsg());
        if (result.getCode() != 0) {
            return R.failure(result.getCode(), result.getMsg());
        }
        saveNotificationRecord(co, notificationId);
        // 当存在不存在的目标用户等提示信息时，保留服务层返回的 msg
        if (StringUtils.isNotBlank(result.getMsg()) && !"成功".equals(result.getMsg())) {
            return R.success(result.getCode(), result.getMsg());
        }
        return R.success();
    }

    private void saveNotificationRecord(SendNotificationCO co, Long notificationId) {
        try {
            List<Long> resolvedUserIds = new ArrayList<>();
            if (StringUtils.isNotBlank(co.getTargetUserIds())) {
                for (String id : co.getTargetUserIds().split(";")) {
                    if (StringUtils.isNotBlank(id)) {
                        try {
                            resolvedUserIds.add(Long.parseLong(id.trim()));
                        } catch (NumberFormatException e) {
                            log.warn("无效的用户ID，跳过：{}", id);
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(co.getTargetIdCards())) {
                try {
                    var userInfo = imUserRpcApi.getUsersInfo(null, co.getTargetIdCards().replace(";", ","), null);
                    if (userInfo != null && userInfo.getResults() != null) {
                        for (var user : userInfo.getResults()) {
                            if (user.getId() != null) {
                                resolvedUserIds.add(user.getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("通过身份证号查询用户ID失败，通知记录可能不完整", e);
                }
            }
            notificationService.saveNotification(
                    co.getAppId(), co.getModuleName(), co.getContent(),
                    co.getCollaborativeMsg(), co.getUrl(), co.getShowInNotification(),
                    notificationId, resolvedUserIds);
        } catch (Exception e) {
            log.error("保存通知记录失败", e);
        }
    }
}