package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.base.api.param.FileStorageDto;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;
import com.tdtech.cloudcmd.base.api.service.FileStorageRpcService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.ImMessageRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.CardMessage;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.IMMsgRspVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.MMSMsgVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendImMessageCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendNotificationCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.UserGetResultVo;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.CardMsgDataVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.CardMsgVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ForwardObj;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImMessageRequest;
import com.tdtech.cloudcmd.im.jingxin.client.entity.NotifyMsgVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.PropertyVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.TxtMsgVo;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@DubboService
public class ImMessageRpc implements ImMessageRpcApi {

    private static final int MSG_TYPE_TEXT = 1;
    private static final int MSG_TYPE_MMS = 2;
    private static final int MSG_TYPE_CARD = 5;
    private static final int MSG_TYPE_NOTIFY = 11;
    private static final int CATEGORY_IM = 4;
    private static final int FORWARD_CATEGORY_CHAT = 1;
    private static final int FORWARD_CATEGORY_GROUP = 2;
    private static final int FORWARD_ID_TYPE_USER = 0;
    private static final int FORWARD_ID_TYPE_GROUP = 2;
    private static final int PLAINTEXT_FLAG = 1;

    /**
     * 文件上传分类（警信 uploadFile 接口 category 参数）：0-头像文件，1-IM附件
     * 彩信附件统一按 IM附件 上传
     */
    private static final int UPLOAD_FILE_CATEGORY_IM = 1;

    private static final String ITEM_INNER_MESSAGE = "ITEM_INNER_MESSAGE";
    private static final String ITEM_POLICE_COOPERATION = "ITEM_POLICE_COOPERATION";
    private static final String CARD_TYPE_CUSTOM = "customCard";

    @Resource
    private ImHttpClient imHttpClient;

    @Resource
    private GroupAiClient groupAiClient;

    @DubboReference
    private ImUserRpcApi imUserRpcApi;

    @DubboReference
    private FileStorageRpcService fileStorageRpcService;

    @Override
    public IMMsgRspVo sendImMessage(SendImMessageCO co) {
        // 参数校验
        IMMsgRspVo validationError = validateMessage(co);
        if (validationError != null) {
            return validationError;
        }

        // 校验：用户ID、身份证号、群组号三选一，不能混填
        IMMsgRspVo targetError = validateTargets(co);
        if (targetError != null) {
            return targetError;
        }

        // X-App-Id 非空：用指定虚拟用户发送；为空：用默认虚拟用户发送
        String appId = co.getAppId();
        boolean useSpecifiedVirtualUser = StringUtils.isNotBlank(appId);
        Long proxyUserId;
        if (useSpecifiedVirtualUser) {
            // 先校验虚拟用户是否存在（轻量查询，不走鉴权），不存在直接返回，避免无意义的 HTTP 鉴权调用
            if (!groupAiClient.existsVirtualUserByAppId(appId)) {
                return errorResponse(7, "不存在的虚拟用户: " + appId);
            }
            // 虚拟用户存在，再走鉴权拿 proxyUserId（已绑定的复用 WS token 缓存，未绑定的走独立鉴权）
            proxyUserId = groupAiClient.getProxyUserIdByAppId(appId);
            if (proxyUserId == null) {
                return errorResponse(-1, "获取虚拟用户ID失败: " + appId);
            }
        } else {
            proxyUserId = imHttpClient.getProxyUserId();
        }
        if (proxyUserId == null) {
            return errorResponse(-1, "获取虚拟用户ID失败");
        }

        // 预构建消息体。彩信需查文件+上传警信换 fileKey，预先执行一次，避免群组场景逐群重复上传
        Object msgVo;
        try {
            msgVo = buildMsgVo(co);
        } catch (Exception e) {
            log.error("构建消息体失败，msgType={}", co.getMsgType(), e);
            return errorResponse(-1, e.getMessage());
        }

        List<IMMsgRspVo.Result> allResults = new ArrayList<>();
        int totalSuccess = 0;
        int totalFail = 0;
        List<String> notFoundUserIds = new ArrayList<>();

        // 发送给用户：category=4，多人转发
        if (StringUtils.isNotBlank(co.getTargetUserIds()) || StringUtils.isNotBlank(co.getTargetIdCards())) {
            ResolvedTargets targets = resolveTargetUserIds(co.getTargetUserIds(), co.getTargetIdCards());
            notFoundUserIds = targets.notFoundIds;
            if (targets.resolvedIds.isEmpty()) {
                return errorResponse(6, "目标用户不存在");
            }
            List<ForwardObj> forwardList = buildForwardListFromUserIds(targets.resolvedIds);
            if (!forwardList.isEmpty()) {
                ImMessageRequest<Object> request = buildMessageRequest(proxyUserId, forwardList, co.getMsgType(), msgVo);
                try {
                    var result = imHttpClient.sendMsgByFrom(request);
                    IMMsgRspVo userRsp = handleCardMessageResult(result, forwardList);
                    if (userRsp.getMultiResult() != null) {
                        allResults.addAll(userRsp.getMultiResult());
                    }
                    if (userRsp.getCode() == 0) {
                        long failCount = userRsp.getMultiResult() != null ?
                                userRsp.getMultiResult().stream().filter(r -> r.getCode() != null && r.getCode() != 0).count() : 0;
                        totalSuccess += forwardList.size() - (int) failCount;
                        totalFail += (int) failCount;
                    } else {
                        totalFail += forwardList.size();
                    }
                } catch (Exception e) {
                    log.error("发送IM卡片消息给用户失败", e);
                    totalFail += forwardList.size();
                }
            }
        }

        // 发送给群组：category=2，逐群发送
        if (StringUtils.isNotBlank(co.getGroupIds())) {
            String[] groupArray = co.getGroupIds().split(";");
            for (String groupId : groupArray) {
                if (StringUtils.isBlank(groupId)) continue;
                try {
                    ImMessageRequest<Object> request = new ImMessageRequest<>();
                    request.setCategory(FORWARD_CATEGORY_GROUP);
                    request.setMsgType(co.getMsgType());
                    request.setFrom(proxyUserId);
                    request.setTo(groupId.trim());
                    request.setMsg(msgVo);
                    request.setPlaintext(PLAINTEXT_FLAG);

                    var result = imHttpClient.sendMsgByFrom(request);
                    IMMsgRspVo.Result groupResult = new IMMsgRspVo.Result();
                    groupResult.setCode(0);
                    groupResult.setMsg("成功");
                    groupResult.setMsgId(result != null ? result.getMsgId() : null);
                    groupResult.setToId(groupId.trim());
                    allResults.add(groupResult);
                    totalSuccess++;
                } catch (Exception e) {
                    log.error("发送IM卡片消息给群组{}失败", groupId.trim(), e);
                    IMMsgRspVo.Result groupResult = new IMMsgRspVo.Result();
                    groupResult.setCode(-1);
                    groupResult.setMsg(e.getMessage());
                    groupResult.setToId(groupId.trim());
                    allResults.add(groupResult);
                    totalFail++;
                }
            }
        }

        IMMsgRspVo rspVo = new IMMsgRspVo();
        rspVo.setMultiResult(allResults);

        String notFoundMsg = !notFoundUserIds.isEmpty()
                ? "部分目标用户" + String.join("、", notFoundUserIds) + "不存在"
                : null;

        if (totalFail == 0) {
            rspVo.setCode(0);
            rspVo.setMsg(notFoundMsg != null ? notFoundMsg : "成功");
        } else {
            rspVo.setCode(-1);
            List<String> failMsgs = allResults.stream()
                    .filter(r -> r.getCode() != null && r.getCode() != 0)
                    .map(r -> {
                        String toId = r.getToId() != null ? r.getToId() : "未知";
                        String errorMsg = r.getMsg() != null ? r.getMsg() : "发送失败";
                        return toId + ": " + errorMsg;
                    })
                    .collect(Collectors.toList());

            StringBuilder msgBuilder = new StringBuilder();
            if (notFoundMsg != null) {
                msgBuilder.append(notFoundMsg).append("；");
            }
            if (!failMsgs.isEmpty()) {
                msgBuilder.append(String.join("、", failMsgs));
            }
            rspVo.setMsg(msgBuilder.toString());
        }

        return rspVo;
    }

    @Override
    public IMMsgRspVo sendNotification(SendNotificationCO co) {
        // 参数校验
        boolean hasTargetUserIds = StringUtils.isBlank(co.getTargetUserIds());
        boolean hasTargetIdCards = StringUtils.isBlank(co.getTargetIdCards());
        if (hasTargetIdCards == hasTargetUserIds) {
            return errorResponse(2, "targetUserIds和targetIdCards二选一必填，且不能混填");
        }

        Long proxyUserId = imHttpClient.getProxyUserId();
        if (proxyUserId == null) {
            return errorResponse(-1, "未知错误");
        }

        ResolvedTargets targets = resolveTargetUserIds(co.getTargetUserIds(), co.getTargetIdCards());
        if (targets.resolvedIds.isEmpty()) {
            return errorResponse(3, "任务通知目标用户不存在");
        }

        List<ForwardObj> forwardList = buildForwardListFromUserIds(targets.resolvedIds);
        ImMessageRequest<NotifyMsgVo> request = buildNotifyMessageRequest(proxyUserId, forwardList, co);

        try {
            var result = imHttpClient.sendMsg(request);
            IMMsgRspVo rsp = handleNotifyMessageResult(result, forwardList.size());
            if (!targets.notFoundIds.isEmpty()) {
                String notFoundMsg = "部分目标用户" + String.join("、", targets.notFoundIds) + "不存在";
                if (rsp.getCode() == 0 && "成功".equals(rsp.getMsg())) {
                    rsp.setMsg(notFoundMsg);
                } else {
                    rsp.setMsg(notFoundMsg + "；" + rsp.getMsg());
                }
            }
            return rsp;
        } catch (Exception e) {
            log.error("发送通知消息失败", e);
            return errorResponse(-1, "未知错误");
        }
    }

    private IMMsgRspVo errorResponse(int code, String msg) {
        IMMsgRspVo rsp = new IMMsgRspVo();
        rsp.setCode(code);
        rsp.setMsg(msg);
        return rsp;
    }

    private IMMsgRspVo validateMessage(SendImMessageCO co) {
        Integer msgType = co.getMsgType();
        if (msgType == null) {
            return errorResponse(3, "消息类型不能为空");
        }
        if (msgType == MSG_TYPE_TEXT) {
            if (StringUtils.isBlank(co.getContent())) {
                return errorResponse(7, "文本内容不能为空");
            }
            return null;
        }
        if (msgType == MSG_TYPE_MMS) {
            if (StringUtils.isBlank(co.getFileId())) {
                return errorResponse(8, "文件ID不能为空");
            }
            return null;
        }
        if (msgType == MSG_TYPE_CARD) {
            if (co.getCard() == null) {
                return errorResponse(4, "卡片内容不能为空");
            }
            return null;
        }
        return errorResponse(3, "不支持的消息类型");
    }

    private IMMsgRspVo validateTargets(SendImMessageCO co) {
        boolean hasUserIds = StringUtils.isNotBlank(co.getTargetUserIds());
        boolean hasIdCards = StringUtils.isNotBlank(co.getTargetIdCards());
        boolean hasGroupIds = StringUtils.isNotBlank(co.getGroupIds());

        // 三选一，不能混填
        int count = (hasUserIds ? 1 : 0) + (hasIdCards ? 1 : 0) + (hasGroupIds ? 1 : 0);
        if (count == 0 || count > 1) {
            return errorResponse(5, "targetUserIds、targetIdCards、groupIds三选一必填，且不能混填");
        }

        return null;
    }

    private List<ForwardObj> buildForwardListFromUserIds(List<Long> userIds) {
        return userIds.stream()
                .map(userId -> createForwardObj(userId, FORWARD_CATEGORY_CHAT, FORWARD_ID_TYPE_USER))
                .collect(Collectors.toList());
    }

    private ForwardObj createForwardObj(Long userId, Integer forwardType, Integer toIdType) {
        ForwardObj obj = new ForwardObj();
        obj.setCategory(forwardType);
        obj.setToObjId(userId);
        obj.setToIdType(toIdType);
        return obj;
    }

    /**
     * 按消息类型构建消息体对象
     * 文本消息(msgType=1)返回 TxtMsgVo；彩信(msgType=2)返回 MMSMsgVo；卡片消息(msgType=5)返回 CardMsgVo；
     * 其他类型在 validateMessage 阶段已拦截，此处不可达
     * 彩信会查文件存储记录并上传警信换取 fileKey，可能抛异常
     */
    private Object buildMsgVo(SendImMessageCO co) {
        Integer msgType = co.getMsgType();
        if (msgType != null && msgType == MSG_TYPE_TEXT) {
            TxtMsgVo txtMsgVo = new TxtMsgVo();
            txtMsgVo.setText(co.getContent());
            return txtMsgVo;
        }
        if (msgType != null && msgType == MSG_TYPE_MMS) {
            return buildMmsMsgVo(co.getFileId());
        }
        if (msgType != null && msgType == MSG_TYPE_CARD) {
            return buildCardMsgVo(co.getCard());
        }
        throw new BusinessException("不支持的消息类型: " + msgType);
    }

    /**
     * 构建彩信消息体：根据 fileId（文件存储记录的 fileMd5）查询文件信息，
     * 读取物理文件并上传至警信换取 fileKey，再填充 MMSMsgVo 必填字段
     *
     * @param fileId 文件存储记录的 fileMd5（openapi 上传接口返回值）
     * @return 彩信消息体
     */
    @Override
    public MMSMsgVo buildMmsMsgVo(String fileId) {
        FileStorageQueryParam queryParam = new FileStorageQueryParam();
        queryParam.setFileMd5(fileId);
        FileStorageDto fileStorage = fileStorageRpcService.getFileStorage(queryParam);
        if (fileStorage == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }
        String storagePath = fileStorage.getStoragePath();
        if (StringUtils.isBlank(storagePath)) {
            throw new BusinessException("文件存储路径为空: " + fileId);
        }
        File file = new File(storagePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException("文件不存在或非文件: " + storagePath);
        }
        String fileName = fileStorage.getFileName();
        String fileKey = imHttpClient.uploadFile(file, fileName, UPLOAD_FILE_CATEGORY_IM);
        if (StringUtils.isBlank(fileKey)) {
            throw new BusinessException("上传文件至警信失败，未获取到 fileKey: " + fileId);
        }
        MMSMsgVo mmsMsgVo = new MMSMsgVo();
        mmsMsgVo.setFileKey(fileKey);
        mmsMsgVo.setFileName(fileName);
        // 文件大小由字节转KB，向上取整
        Long fileSizeBytes = fileStorage.getFileSize();
        mmsMsgVo.setFileSize(fileSizeBytes);
        mmsMsgVo.setFileType(mapFileType(fileStorage.getMimeType()));
        return mmsMsgVo;
    }

    /**
     * MIME 类型映射为警信彩信 fileType
     * 1-image；2-audio；3-video；4-general；5-word；6-excel；7-pdf；8-txt；10-ppt
     */
    private Integer mapFileType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return 4;
        }
        String lower = mimeType.toLowerCase();
        if (lower.startsWith("image/")) {
            return 1;
        }
        if (lower.startsWith("audio/")) {
            return 2;
        }
        if (lower.startsWith("video/")) {
            return 3;
        }
        if (lower.contains("pdf")) {
            return 7;
        }
        if (lower.contains("msword") || lower.contains("wordprocessing")) {
            return 5;
        }
        if (lower.contains("ms-excel") || lower.contains("spreadsheetml")) {
            return 6;
        }
        if (lower.contains("ms-powerpoint") || lower.contains("presentationml")) {
            return 10;
        }
        if (lower.startsWith("text/")) {
            return 8;
        }
        return 4;
    }

    private ImMessageRequest<Object> buildMessageRequest(Long proxyUserId, List<ForwardObj> forwardList,
                                                         Integer msgType, Object msgVo) {
        ImMessageRequest<Object> request = new ImMessageRequest<>();
        request.setCategory(CATEGORY_IM);
        request.setMsgType(msgType);
        request.setFrom(proxyUserId);
        request.setForwardList(forwardList);
        request.setMsg(msgVo);
        return request;
    }

    private CardMsgVo buildCardMsgVo(CardMessage card) {
        CardMsgVo cardMsgVo = new CardMsgVo();
        cardMsgVo.setCardType(CARD_TYPE_CUSTOM);

        CardMsgDataVo dataVo = new CardMsgDataVo();
        dataVo.setEmergencyLevel(card.getLevel());
        dataVo.setThumb(card.getThumb());
        dataVo.setTitle(card.getTitle());
        dataVo.setDescribe(card.getDescribe());
        dataVo.setUrl(card.getUrl());

        cardMsgVo.setData(dataVo);
        return cardMsgVo;
    }

    private IMMsgRspVo handleCardMessageResult(com.tdtech.cloudcmd.im.jingxin.client.entity.IMMsgRspVo result, List<ForwardObj> forwardList) {
        IMMsgRspVo rspVo = new IMMsgRspVo();
        if (result == null) {
            return errorResponse(-1, "发送失败：未获取到响应");
        }

        List<IMMsgRspVo.Result> apiResultList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        if (result.getMultiResult() != null && !result.getMultiResult().isEmpty()) {
            List<ForwardObj> objs = forwardList != null ? forwardList : Collections.emptyList();
            for (int i = 0; i < result.getMultiResult().size(); i++) {
                var clientResult = result.getMultiResult().get(i);
                IMMsgRspVo.Result apiResult = new IMMsgRspVo.Result();
                apiResult.setCode(clientResult.getCode());
                apiResult.setMsg(clientResult.getMsg());
                apiResult.setMsgId(clientResult.getMsgId());
                // 回填接收人信息（IM返回的multiResult与forwardList顺序一致）
                if (i < objs.size()) {
                    apiResult.setToId(objs.get(i).getToObjId() != null ? String.valueOf(objs.get(i).getToObjId()) : null);
                }
                apiResultList.add(apiResult);

                if (clientResult.getCode() != null && clientResult.getCode() == 0) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        } else if (StringUtils.isNotBlank(result.getMsgId())) {
            IMMsgRspVo.Result apiResult = new IMMsgRspVo.Result();
            apiResult.setCode(0);
            apiResult.setMsg("成功");
            apiResult.setMsgId(result.getMsgId());
            // 单条转发场景，回填唯一接收人
            if (forwardList != null && !forwardList.isEmpty()) {
                apiResult.setToId(forwardList.get(0).getToObjId() != null ? String.valueOf(forwardList.get(0).getToObjId()) : null);
            }
            apiResultList.add(apiResult);
            successCount = 1;
        } else {
            failCount = 1;
        }

        rspVo.setMultiResult(apiResultList);

        if (failCount == 0) {
            rspVo.setCode(0);
            rspVo.setMsg("成功");
        } else if (successCount > 0) {
            rspVo.setCode(0);
            rspVo.setMsg(String.format("部分成功：成功%d人，失败%d人", successCount, failCount));
        } else {
            rspVo.setCode(-1);
            rspVo.setMsg("发送失败：全部接收者发送失败");
        }

        return rspVo;
    }

    /** 用户解析结果：已解析的用户ID + 不存在的用户标识 */
    private static class ResolvedTargets {
        final List<Long> resolvedIds = new ArrayList<>();
        final List<String> notFoundIds = new ArrayList<>();
    }

    private ResolvedTargets resolveTargetUserIds(String targetUserIds, String targetIdCards) {
        ResolvedTargets targets = new ResolvedTargets();

        if (StringUtils.isNotBlank(targetUserIds)) {
            List<String> validIdStrs = new ArrayList<>();
            for (String id : targetUserIds.split(";")) {
                if (StringUtils.isBlank(id)) continue;
                String trimmed = id.trim();
                try {
                    Long.parseLong(trimmed);
                    validIdStrs.add(trimmed);
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID，跳过：{}", id);
                    targets.notFoundIds.add(trimmed);
                }
            }
            if (!validIdStrs.isEmpty()) {
                queryUserIdsByUserIds(validIdStrs, targets);
            }
        }

        if (StringUtils.isNotBlank(targetIdCards)) {
            List<String> idCardList = Arrays.stream(targetIdCards.split(";"))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toList());

            queryUserIdsByIdCards(idCardList, targets);
        }

        return targets;
    }

    private void queryUserIdsByUserIds(List<String> userIdStrs, ResolvedTargets targets) {
        if (userIdStrs.isEmpty()) {
            return;
        }
        try {
            UserGetResultVo userInfo = imUserRpcApi.getUsersInfo(String.join(",", userIdStrs), null, null);
            if (userInfo != null && userInfo.getResults() != null) {
                java.util.Set<String> foundUserIds = new java.util.HashSet<>();
                for (var user : userInfo.getResults()) {
                    if (user.getId() != null) {
                        targets.resolvedIds.add(user.getId());
                        foundUserIds.add(String.valueOf(user.getId()));
                    }
                }
                // 输入的 userId 未在查询结果中出现 → 不存在
                for (String userId : userIdStrs) {
                    if (!foundUserIds.contains(userId)) {
                        targets.notFoundIds.add(userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("通过用户ID查询用户失败, userIds: {}", userIdStrs, e);
        }
    }

    private void queryUserIdsByIdCards(List<String> idCardList, ResolvedTargets targets) {
        if (idCardList.isEmpty()) {
            return;
        }

        try {
            UserGetResultVo userInfo = imUserRpcApi.getUsersInfo(null, String.join(",", idCardList), null);
            if (userInfo != null && userInfo.getResults() != null) {
                Set<String> foundIdCards = new HashSet<>();
                for (var user : userInfo.getResults()) {
                    if (user.getId() != null) {
                        targets.resolvedIds.add(user.getId());
                    }
                    if (StringUtils.isNotBlank(user.getIdCard())) {
                        foundIdCards.add(user.getIdCard());
                    }
                }
                // 输入的 idCard 未在查询结果中出现 → 不存在
                for (String idCard : idCardList) {
                    if (!foundIdCards.contains(idCard)) {
                        targets.notFoundIds.add(idCard);
                    }
                }
                // IM 查询接口返回的 failures 也是不存在的用户
                if (userInfo.getFailures() != null && !userInfo.getFailures().isEmpty()) {
                    for (var fail : userInfo.getFailures()) {
                        if (StringUtils.isNotBlank(fail.getIdCard()) && !targets.notFoundIds.contains(fail.getIdCard())) {
                            targets.notFoundIds.add(fail.getIdCard());
                        }
                    }
                    log.warn("部分身份证号查询用户失败: {}", userInfo.getFailures().stream()
                            .map(f -> f.getIdCard() + ":" + f.getErrMsg())
                            .collect(Collectors.joining(", ")));
                }
            }
        } catch (Exception e) {
            log.error("通过身份证号查询用户ID失败, idCards: {}", idCardList, e);
        }
    }

    private ImMessageRequest<NotifyMsgVo> buildNotifyMessageRequest(Long proxyUserId, List<ForwardObj> forwardList, SendNotificationCO co) {
        ImMessageRequest<NotifyMsgVo> request = new ImMessageRequest<>();
        request.setCategory(CATEGORY_IM);
        request.setMsgType(MSG_TYPE_NOTIFY);
        request.setFrom(proxyUserId);
        request.setForwardList(forwardList);
        request.setCMsgId(UUID.randomUUID().toString().replace("-", ""));
        request.setPlaintext(PLAINTEXT_FLAG);
        request.setMsg(buildNotifyMsgVo(co));
        return request;
    }

    private NotifyMsgVo buildNotifyMsgVo(SendNotificationCO co) {
        NotifyMsgVo notifyMsgVo = new NotifyMsgVo();
        List<PropertyVo> properties = new ArrayList<>();

        properties.add(createProperty("text", "String", co.getContent()));

        if (StringUtils.isNotBlank(co.getUrl())) {
            properties.add(createProperty("url", "String", co.getUrl()));
        }

        String itemKey = co.getCollaborativeMsg() == 0 ? ITEM_INNER_MESSAGE : ITEM_POLICE_COOPERATION;
        properties.add(createProperty("itemkey", "String", itemKey));

        if (co.getCollaborativeMsg() != null && co.getCollaborativeMsg() == 1 && co.getShowInNotification() != null) {
            properties.add(createProperty("showInNotification", "String", String.valueOf(co.getShowInNotification())));
        }

        if (co.getNotificationId() != null) {
            properties.add(createProperty("notificationId", "String", String.valueOf(co.getNotificationId())));
        }

        notifyMsgVo.setProperties(properties);
        return notifyMsgVo;
    }

    private PropertyVo createProperty(String name, String type, String value) {
        PropertyVo property = new PropertyVo();
        property.setPropertyName(name);
        property.setPropertyType(type);
        property.setPropertyValue(value);
        return property;
    }

    private IMMsgRspVo handleNotifyMessageResult(com.tdtech.cloudcmd.im.jingxin.client.entity.IMMsgRspVo result,
                                                  int totalRecipients) {
        IMMsgRspVo rspVo = new IMMsgRspVo();

        if (result == null) {
            return errorResponse(-1, "发送失败：未获取到响应");
        }

        List<IMMsgRspVo.Result> apiResultList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        if (result.getMultiResult() != null && !result.getMultiResult().isEmpty()) {
            for (var clientResult : result.getMultiResult()) {
                IMMsgRspVo.Result apiResult = new IMMsgRspVo.Result();
                apiResult.setCode(clientResult.getCode());
                apiResult.setMsg(clientResult.getMsg());
                apiResult.setMsgId(clientResult.getMsgId());
                apiResultList.add(apiResult);

                if (clientResult.getCode() != null && clientResult.getCode() == 0) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        } else if (StringUtils.isNotBlank(result.getMsgId())) {
            IMMsgRspVo.Result apiResult = new IMMsgRspVo.Result();
            apiResult.setCode(0);
            apiResult.setMsg("成功");
            apiResult.setMsgId(result.getMsgId());
            apiResultList.add(apiResult);
            successCount = totalRecipients;
        } else {
            failCount = totalRecipients;
        }

        rspVo.setMultiResult(apiResultList);

        if (failCount == 0) {
            rspVo.setCode(0);
            rspVo.setMsg("成功");
        } else if (successCount > 0) {
            rspVo.setCode(0);
            rspVo.setMsg(String.format("部分成功：成功%d人，失败%d人", successCount, failCount));
        } else {
            rspVo.setCode(-1);
            rspVo.setMsg("发送失败：全部接收者发送失败");
        }

        return rspVo;
    }
}