package com.tdtech.cloudcmd.im.jingxin.server.aiclient.group;

import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.client.ws.WebSocketMessageHandler;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.ReactAiAgentClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AskAIReq;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.IMMsgToAIReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupAiBindEntity;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.MimeTypeUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.advice.SystemException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ChannelHandler.Sharable
public class GroupAiWebSocketMessageHandler extends WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(GroupAiWebSocketMessageHandler.class);

    private static final String END_FLAG = "[end]";
    private static final String ERROR_FLAG = "[error]";

    private static final String NOTIFY_TYPE_TEXT_MSG = "TEXT_MSG";
    private static final String NOTIFY_TYPE_MEDIA_MSG = "MEDIA_MSG";

    /**
     * MMSMsgVo.fileType 取值：1-image（图片）
     * 仅图片类型才推送给 AI Agent，其他媒体类型（语音/视频/文档等）忽略
     */
    private static final Integer FILE_TYPE_IMAGE = 1;

    /**
     * category 取值：1-单聊；2-群聊
     */
    private static final Integer CATEGORY_SINGLE = 1;
    private static final Integer CATEGORY_GROUP = 2;

    /**
     * askType 取值：3-单聊透传；2-群聊@消息；4-群聊普通消息（非@）
     */
    private static final Integer ASK_TYPE_SINGLE = 3;
    private static final Integer ASK_TYPE_GROUP = 2;
    private static final Integer ASK_TYPE_GROUP_NORMAL = 4;

    private static final Pattern AT_ALL_PATTERN =
        Pattern.compile("\\[@[^]]*?:@all\\]");
    private static final Pattern AT_MENTION_PATTERN =
        Pattern.compile("\\[@[^]]*?:@([^]]*?)\\]");

    private final Pattern atPattern;

    private final Long proxyUserId;

    private final ImHttpClient coopImHttpClient;

    private final GroupAiClient groupAiClient;

    private final ReactAiAgentClient reactAiAgentClient;

    private final ReportUtil reportUtil;

    private final StreamBridge streamBridge;

    private final ImUserVirtualMapper imUserVirtualMapper;

    GroupAiWebSocketMessageHandler(Long proxyUserId, ImHttpClient coopImHttpClient, GroupAiClient groupAiClient,
                                   ReactAiAgentClient reactAiAgentClient, ReportUtil reportUtil,
                                   StreamBridge streamBridge, ImUserVirtualMapper imUserVirtualMapper) {
        this.proxyUserId = proxyUserId;
        this.coopImHttpClient = coopImHttpClient;
        this.groupAiClient = groupAiClient;
        this.reactAiAgentClient = reactAiAgentClient;
        this.reportUtil = reportUtil;
        this.streamBridge = streamBridge;
        this.imUserVirtualMapper = imUserVirtualMapper;
        this.atPattern = Pattern.compile("\\[@" + proxyUserId + ":@[^]]*?](.*)$");
    }

    @Override
    public void onTextMessage(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame msg) {
        var text = msg.text();
        var wsResponse = JsonUtil.parseJson(text, WsResponse.class);
        try {
            if (wsResponse == null) {
                throw new SystemException("parse group msg error:" + text);
            }
            if (Objects.equals(wsResponse.getNotifyType(), "pong")) {
                logger.info("pong:{}", text);
                return;
            }
            if (wsResponse.getData() == null) {
                logger.warn("empty group data:{}", text);
                return;
            }
            logger.info("receive group message:{}", text);
            if (wsResponse.getNeedAck() != null && wsResponse.getNeedAck()) {
                var wsAck = new WsAck(wsResponse);
                channelHandlerContext.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJsonStr(wsAck)));
            }
            if (!Objects.equals(wsResponse.getModule(), "im")) {
                return;
            }
            var notifyType = wsResponse.getNotifyType();

            // 查询虚拟用户绑定信息，未绑定则不处理
            var virtualUserBind = resolveVirtualUserBind();
            if (virtualUserBind == null) {
                return;
            }

            // 查询智能体配置，区分三种模式：透传 / 原@模式 / 完全不处理
            var agentConfig = virtualUserBind.getAgentId() != null
                    ? reactAiAgentClient.queryAgentConfig(virtualUserBind.getAgentId())
                    : null;

            // scope=2(仅IM) 且 receiveIm!=1(不接收IM)：agent 既不做问答也不接收IM，完全不处理
            if (reactAiAgentClient.shouldSkipIm(agentConfig)) {
                logger.info("skip im message:{}", text);
                return;
            }

            boolean transparentMode = reactAiAgentClient.isTransparentMode(agentConfig);

            if (!transparentMode) {
                // 原 @ 模式：仅处理文本消息
                if (!Objects.equals(notifyType, NOTIFY_TYPE_TEXT_MSG)) {
                    return;
                }
                var sourceMsg = JsonUtil.convert(wsResponse.getData(), WsTextMessage.class);
                Integer category = sourceMsg.getCategory();
                // 单聊：所有消息都推送给 AI Agent
                if (Objects.equals(category, CATEGORY_SINGLE)) {
                    String content = sourceMsg.getMsg() != null ? sourceMsg.getMsg().getText() : "";
                    processAtGroupAIMsg(content, sourceMsg, ASK_TYPE_SINGLE);
                    return;
                }
                // 群聊：仅处理 @本AI 的消息
                String question = isAtGroupAIMsg(sourceMsg);
                logger.info("imTextMessage isAtGroupAIMsg: {}", question);
                if (question != null) {
                    processAtGroupAIMsg(question, sourceMsg, ASK_TYPE_GROUP);
                }
                return;
            }
            // 透传模式：处理单聊/群聊的文本和图片消息
            // 使用 IMOfflineMsgItemVo 解析：msg 为 Object 类型，可按文本/图片分别转换为 TxtOfflineMsgVo / MMSMsgVo
            if (!Objects.equals(notifyType, NOTIFY_TYPE_TEXT_MSG) && !Objects.equals(notifyType, NOTIFY_TYPE_MEDIA_MSG)) {
                return;
            }
            var sourceMsg = JsonUtil.convert(wsResponse.getData(), IMOfflineMsgItemVo.class);
            Integer category = sourceMsg.getCategory();
            // 单聊模式
            if (Objects.equals(category, CATEGORY_SINGLE)) {
                processSingleChatMsg(sourceMsg, notifyType, virtualUserBind);
                return;
            }
            // 群聊模式
            if (Objects.equals(category, CATEGORY_GROUP)) {
                processGroupChatMsg(sourceMsg, notifyType, virtualUserBind);
                return;
            }
        } catch (Exception e) {
            if (wsResponse != null && Boolean.TRUE.equals(wsResponse.getNeedAck())) {
                var wsAck = new WsAck(wsResponse);
                wsAck.setMsg(e.getMessage());
                wsAck.setCode(1);
                channelHandlerContext.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJsonStr(wsAck)));
            }
            logger.error("group handle error", e);
        }
    }

    @Override
    public void onHandShakeFinished(ChannelHandlerContext channelHandlerContext) {
        // nope
    }

    private String isAtGroupAIMsg(WsTextMessage wsTextMessage) {
        if (wsTextMessage == null || wsTextMessage.getMsg() == null || StringUtils.isBlank(wsTextMessage.getMsg().getText())) {
            return null;
        }
        Matcher matcher = atPattern.matcher(wsTextMessage.getMsg().getText());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 根据已解析的文本判断是否 @本 AI
     * @param text 文本内容
     * @return @本AI 的问题内容；非@返回 null
     */
    private String isAtGroupAIMsgByText(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        Matcher matcher = atPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 清理 IM @提及格式，将 [@userId:@xxx] 转为可读文本
     * [@xxx:@all] → @所有人
     * [@xxx:@yyy] → @yyy
     */
    private String formatAtMentions(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        // [@xxx:@all] → @所有人 + 空格 + 后面内容
        // [@xxx:@yyy] → @yyy + 空格 + 后面内容
        String replaced = AT_ALL_PATTERN.matcher(text).replaceAll("@所有人 ");
        replaced = AT_MENTION_PATTERN.matcher(replaced).replaceAll("@$1 ");
        return replaced.trim();
    }

    /**
     * 解析媒体消息中的图片
     * 仅当 fileType == 1（image）时返回 MMSMsgVo，其他媒体类型（语音/视频/文档等）返回 null
     */
    private IMOfflineMsgItemVo.MMSMsgVo parseImageMsg(IMOfflineMsgItemVo sourceMsg) {
        if (sourceMsg == null || sourceMsg.getMsg() == null) {
            return null;
        }
        var mmsMsg = JsonUtil.convert(sourceMsg.getMsg(), IMOfflineMsgItemVo.MMSMsgVo.class);
        if (mmsMsg == null || !Objects.equals(mmsMsg.getFileType(), FILE_TYPE_IMAGE)) {
            return null;
        }
        return mmsMsg;
    }

    /**
     * 下载图片字节数据
     * @param fileKey 文件 ID
     * @return 图片字节数组；下载失败返回 null
     */
    private byte[] downloadImageBytes(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return null;
        }
        try {
            byte[] imageBytes = coopImHttpClient.downloadIcon(fileKey);
            if (imageBytes == null || imageBytes.length == 0) {
                logger.warn("download image empty, fileKey:{}", fileKey);
                return null;
            }
            return imageBytes;
        } catch (Exception e) {
            logger.error("download image error, fileKey:{}", fileKey, e);
            return null;
        }
    }

    /**
     * 根据文件名和文件内容推断 MIME 类型
     * 基于文件内容+文件名综合检测，识别失败时按图片场景兜底为 image/jpeg
     */
    private String resolveMimeType(String fileName, byte[] data) {
        String mimeType = MimeTypeUtils.getMimeType(data, fileName);
        return MimeTypeUtils.APPLICATION_OCTET_STREAM.equals(mimeType) ? "image/jpeg" : mimeType;
    }

    /**
     * 解析文本消息中引用的图片
     * 文本消息的 srcMsgId 指向一条图片消息，通过 offlineMsg 接口拉取并解析
     * @return 引用的图片信息；无引用或非图片引用返回 null
     */
    private IMOfflineMsgItemVo.MMSMsgVo parseQuotedImage(IMOfflineMsgItemVo sourceMsg,
                                                          IMOfflineMsgItemVo.TxtOfflineMsgVo txtMsg) {
        if (sourceMsg == null || txtMsg == null || StringUtils.isBlank(txtMsg.getSrcMsgId())) {
            return null;
        }
        try {
            Long srcMsgId = Long.valueOf(txtMsg.getSrcMsgId());
            var offlinePage = coopImHttpClient.offlineMsg(srcMsgId);
            if (offlinePage == null || CollectionUtils.isEmpty(offlinePage.getImMsgs())) {
                logger.warn("quoted image msg not found, srcMsgId:{}", srcMsgId);
                return null;
            }
            var offlineMsg = offlinePage.getImMsgs().get(0);
            if (!Objects.equals(offlineMsg.getNotifyType(), NOTIFY_TYPE_MEDIA_MSG)) {
                return null;
            }
            var quotedItem = offlineMsg.getData();
            if (quotedItem == null) {
                return null;
            }
            var imageMsg = JsonUtil.convert(quotedItem.getMsg(), IMOfflineMsgItemVo.MMSMsgVo.class);
            if (imageMsg == null || !Objects.equals(imageMsg.getFileType(), FILE_TYPE_IMAGE)) {
                return null;
            }
            return imageMsg;
        } catch (NumberFormatException e) {
            logger.warn("parse srcMsgId failed, srcMsgId:{}", txtMsg.getSrcMsgId());
            return null;
        } catch (Exception e) {
            logger.error("parse quoted image error, srcMsgId:{}", txtMsg.getSrcMsgId(), e);
            return null;
        }
    }

    /**
     * 处理原模式消息（单聊所有消息 / 群聊@AI消息）
     * @param question 提问内容；群聊时为 @AI 后的问题，单聊时为完整消息文本
     * @param sourceMsg 原始消息
     * @param askType ASK_TYPE_SINGLE 或 ASK_TYPE_GROUP
     */
    private void processAtGroupAIMsg(String question, WsTextMessage sourceMsg, Integer askType) {
        var virtualUserBind = resolveVirtualUserBind();
        if (virtualUserBind == null) {
            return;
        }
        String virtualUserAppId = virtualUserBind.getAppId();

        if (sourceMsg.getMsg() == null) {
            logger.warn("Invalid source message");
            return;
        }

        // 单聊回复目标为对方(from)，群聊回复目标为群(to)
        String to = Objects.equals(askType, ASK_TYPE_SINGLE) ? String.valueOf(sourceMsg.getFrom()) : sourceMsg.getTo();
        // 单聊/群聊对应的警信侧 category：1-单聊；2-群聊
        Integer category = Objects.equals(askType, ASK_TYPE_SINGLE) ? CATEGORY_SINGLE : CATEGORY_GROUP;

        logger.info("processOriginalMsg askType:{} content: {}", askType, question);
        if (question.isBlank()) {
            String hint = Objects.equals(askType, ASK_TYPE_SINGLE) ? "请输入您的问题" : "提问方式为@xx+问题";
            sendMsg(virtualUserAppId, proxyUserId, hint, to, sourceMsg.getMsgId(), category);
            return;
        }

        sendMsg(virtualUserAppId, proxyUserId, "AI助手处理中", to, sourceMsg.getMsgId(), category);

        // fromRealUserId：主叫真实用户id。仅在category取值为2(群聊消息)，且主叫为协同岗时填写。此时from填写为协同岗虚拟id。
        Long fromRealUserId = sourceMsg.getFromRealUserId();
        Long userId = Objects.nonNull(fromRealUserId) ? fromRealUserId : sourceMsg.getFrom();
        var userInfo = coopImHttpClient.userPage(null, userId + "").getResults();
        if (CollectionUtils.isEmpty(userInfo)) {
            sendMsg(virtualUserAppId, proxyUserId, "系统错误", to, sourceMsg.getMsgId(), category);
            logger.info("processOriginalMsg userInfo is null");
            return;
        }
        var imUser = userInfo.get(0);

        reactAiAgentClient.askAI(buildAIReq(imUser, question, virtualUserBind.getAgentId(), askType), detail -> {
            try {
                String msg;
                if (detail.endsWith(END_FLAG)) {
                    msg = detail.replace(END_FLAG, "");

                    // 擦除告警
                    clearAlarm();
                } else if (detail.endsWith(ERROR_FLAG)) {
                    msg = "系统错误";

                    // 上报告警
                    reportAlarm();
                } else {
                    logger.warn("meet strange resp:{}", detail);
                    msg = "系统错误";

                    // 上报告警
                    reportAlarm();
                }
                sendMsg(virtualUserAppId, proxyUserId, msg, to, sourceMsg.getMsgId(), category);
                sendToCAgent(msg);
            } catch (Exception e) {
                logger.error("Group AI handle error", e);
                sendMsg(virtualUserAppId, proxyUserId, "系统错误", to, sourceMsg.getMsgId(), category);
            }
        });
    }

    private void reportAlarm() {
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.THE_AGENT_IS_DISCONNECTED);
    }

    private void clearAlarm() {
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.THE_AGENT_IS_DISCONNECTED);
    }
    /**
     * 消息解析上下文：一次解析、多处复用，避免重复 JsonConvert / HTTP 查询
     */
    private static class MsgContext {
        String notifyType;
        String content;                              // formatAtMentions 后的文本（图片消息为空）
        String atQuestion;                           // @本AI的问题，null表示非@本AI
        IMOfflineMsgItemVo.MMSMsgVo imageMsg;        // 直接图片（MEDIA_MSG且fileType=1），null表示无
        IMOfflineMsgItemVo.MMSMsgVo quotedImage;     // 文本消息引用的图片，null表示无
        GroupVo groupVo;                             // 群详情（群聊时查询），null表示单聊或查询失败
        byte[] imageBytes;                           // 图片字节（imageMsg 或 quotedImage 下载结果）
    }

    /**
     * 一次性解析消息上下文：文本内容、@状态、图片信息、群详情
     * 后续所有方法复用此对象，避免重复解析和重复 HTTP 查询
     * @return 解析上下文；非图片媒体类型返回 null（调用方应跳过）
     */
    private MsgContext parseMsgContext(IMOfflineMsgItemVo sourceMsg, String notifyType) {
        MsgContext ctx = new MsgContext();
        ctx.notifyType = notifyType;

        if (Objects.equals(notifyType, NOTIFY_TYPE_TEXT_MSG)) {
            var txtMsg = JsonUtil.convert(sourceMsg.getMsg(), IMOfflineMsgItemVo.TxtOfflineMsgVo.class);
            if (txtMsg != null) {
                ctx.content = formatAtMentions(txtMsg.getText());
                ctx.atQuestion = isAtGroupAIMsgByText(txtMsg.getText());
                // 不处理图片：注释掉引用图片解析和下载
                // ctx.quotedImage = parseQuotedImage(sourceMsg, txtMsg);
                // if (ctx.quotedImage != null) {
                //     ctx.imageBytes = downloadImageBytes(ctx.quotedImage.getFileKey());
                // }
            }
        } else if (Objects.equals(notifyType, NOTIFY_TYPE_MEDIA_MSG)) {
            // 不处理图片：所有媒体消息跳过
            return null;
            // ctx.imageMsg = parseImageMsg(sourceMsg);
            // if (ctx.imageMsg == null) {
            //     // 非图片媒体类型（语音/视频/文档等）：忽略
            //     return null;
            // }
            // ctx.content = "";
            // ctx.imageBytes = downloadImageBytes(ctx.imageMsg.getFileKey());
        }

        // 群聊场景：一次性查群详情，供 queryImUser / resolveCoopPostUser / buildIMMsgToAIReq 复用
        if (Objects.equals(sourceMsg.getCategory(), CATEGORY_GROUP) && sourceMsg.getTo() != null) {
            try {
                ctx.groupVo = coopImHttpClient.queryGroupDetail(Long.valueOf(sourceMsg.getTo()));
            } catch (Exception e) {
                logger.warn("query group detail failed, to:{}", sourceMsg.getTo(), e);
            }
        }
        return ctx;
    }

    /**
     * 处理单聊消息（文本/图片）
     * 单聊所有消息都推送给 AI Agent 并回复给对方
     */
    private void processSingleChatMsg(IMOfflineMsgItemVo sourceMsg, String notifyType,
                                       GroupAiBindEntity virtualUserBind) {
        if (sourceMsg.getMsg() == null) {
            logger.warn("single chat: invalid source message, msgId:{}", sourceMsg.getMsgId());
            return;
        }
        MsgContext ctx = parseMsgContext(sourceMsg, notifyType);
        if (ctx == null) {
            logger.info("single chat: skip non-image media msg, msgId:{}", sourceMsg.getMsgId());
            return;
        }

        String virtualUserAppId = virtualUserBind.getAppId();
        Long agentId = virtualUserBind.getAgentId();
        String to = sourceMsg.getFrom();

        ImUser imUser = queryImUser(sourceMsg, ctx);

        sendMsg(virtualUserAppId, proxyUserId, "AI助手处理中", to, sourceMsg.getMsgId(), CATEGORY_SINGLE);
        askAiAndReply(imUser, ctx.content, agentId, ASK_TYPE_SINGLE,
                virtualUserAppId, to, sourceMsg.getMsgId(), sourceMsg, ctx);
    }

    /**
     * 处理群聊消息（文本/图片）
     * - @ 本 AI：推送 + 回复（按原逻辑）
     * - 普通 @ 不本 AI 或非 @ 消息：推送但不回复
     */
    private void processGroupChatMsg(IMOfflineMsgItemVo sourceMsg, String notifyType,
                                      GroupAiBindEntity virtualUserBind) {
        if (sourceMsg.getMsg() == null) {
            logger.warn("group chat: invalid source message, msgId:{}", sourceMsg.getMsgId());
            return;
        }
        MsgContext ctx = parseMsgContext(sourceMsg, notifyType);
        if (ctx == null) {
            logger.info("group chat: skip non-image media msg, msgId:{}", sourceMsg.getMsgId());
            return;
        }

        String virtualUserAppId = virtualUserBind.getAppId();
        Long agentId = virtualUserBind.getAgentId();
        String to = sourceMsg.getTo();
        ImUser imUser = queryImUser(sourceMsg, ctx);

        if (ctx.atQuestion != null) {
            // @本 AI：推送 + 回复
            if (ctx.atQuestion.isBlank()) {
                sendMsg(virtualUserAppId, proxyUserId, "提问方式为@xx+问题", to, sourceMsg.getMsgId(), CATEGORY_GROUP);
                return;
            }
            sendMsg(virtualUserAppId, proxyUserId, "AI助手处理中", to, sourceMsg.getMsgId(), CATEGORY_GROUP);
            askAiAndReply(imUser, ctx.atQuestion, agentId, ASK_TYPE_GROUP,
                    virtualUserAppId, to, sourceMsg.getMsgId(), sourceMsg, ctx);
        } else {
            // 非 @ 或 @ 其他 AI：推送但不回复
            askAiNoReply(imUser, ctx.content, agentId, ASK_TYPE_GROUP_NORMAL, sourceMsg, ctx);
        }
    }

    /**
     * 查询 IM 用户信息
     * @return 查询不到返回兜底用户，保证不为 null
     */
    /**
     * 查询虚拟用户绑定信息
     * @return 绑定信息；查询失败或未绑定时返回 null
     */
    private GroupAiBindEntity resolveVirtualUserBind() {
        String virtualUserAppId = groupAiClient.getUserByProxyUser(proxyUserId);
        if (virtualUserAppId == null) {
            logger.error("can not find proxy user {} and client bind info", proxyUserId);
            return null;
        }
        var virtualUserInfo = imUserVirtualMapper.selectGroupAiBind(virtualUserAppId);
        if (virtualUserInfo == null || virtualUserInfo.getAgentId() == null) {
            logger.error("can not find virtual user {} bind info, remove it", virtualUserAppId);
            groupAiClient.removeUser(virtualUserAppId);
            return null;
        }
        return virtualUserInfo;
    }

    private ImUser queryImUser(IMOfflineMsgItemVo sourceMsg, MsgContext ctx) {
        // fromRealUserId：主叫真实用户id。仅在category取值为2(群聊消息)，且主叫为协同岗时填写。此时from填写为协同岗虚拟id。
        String fromRealUserId = sourceMsg.getFromRealUserId();
        String userId = StringUtils.isNotBlank(fromRealUserId) ? fromRealUserId : sourceMsg.getFrom();
        if (StringUtils.isBlank(userId)) {
            logger.warn("query im user: from is null, msgId:{}", sourceMsg.getMsgId());
            return buildFallbackUser(sourceMsg);
        }

        // 优先通过 userPage 查询
        var userInfo = coopImHttpClient.userPage(null, userId).getResults();
        if (!CollectionUtils.isEmpty(userInfo)) {
            return userInfo.get(0);
        }

        // 群聊场景：userPage 查不到（可能是虚拟用户），从 ctx 复用群详情查群成员
        if (ctx.groupVo != null && !CollectionUtils.isEmpty(ctx.groupVo.getGroupMembers())) {
            Long matchUserId = Long.valueOf(userId);
            for (var member : ctx.groupVo.getGroupMembers()) {
                if (matchUserId.equals(member.getUserId())) {
                    ImUser imUser = new ImUser();
                    imUser.setId(member.getUserId());
                    imUser.setName(StringUtils.isNotBlank(member.getName()) ? member.getName() : "未知用户");
                    imUser.setIdCard(member.getIdCard());
                    return imUser;
                }
            }
        }

        logger.warn("query im user not found, userId:{}, msgId:{}", userId, sourceMsg.getMsgId());
        return buildFallbackUser(sourceMsg);
    }

    /**
     * 构建兜底用户信息，确保消息流程不因用户查询失败而中断
     */
    private ImUser buildFallbackUser(IMOfflineMsgItemVo sourceMsg) {
        ImUser fallback = new ImUser();
        String from = sourceMsg.getFrom();
        fallback.setId(StringUtils.isNotBlank(from) ? Long.valueOf(from) : 0L);
        fallback.setName("未知用户");
        fallback.setIdCard(null);
        return fallback;
    }

    /**
     * 解析协同岗用户信息，格式：协同岗名称(协同岗ID)
     * 从 ctx 复用群成员信息查协同岗名称
     */
    private String resolveCoopPostUser(IMOfflineMsgItemVo sourceMsg, MsgContext ctx) {
        String coopPostId = sourceMsg.getFrom();
        if (ctx.groupVo != null && !CollectionUtils.isEmpty(ctx.groupVo.getGroupMembers())) {
            Long matchUserId = Long.valueOf(coopPostId);
            for (var member : ctx.groupVo.getGroupMembers()) {
                if (matchUserId.equals(member.getUserId())) {
                    String name = StringUtils.isNotBlank(member.getName()) ? member.getName() : "未知协同岗";
                    return name + "(" + coopPostId + ")";
                }
            }
        }
        // 兜底：用协同岗ID作为名称
        return "协同岗(" + coopPostId + ")";
    }

    /**
     * 调用 AI Agent 并回复给 IM
     */
    private void askAiAndReply(ImUser imUser, String content, Long agentId, Integer askType,
                               String virtualUserAppId, String to, String srcMsgId,
                               IMOfflineMsgItemVo sourceMsg, MsgContext ctx) {
        // 单聊/群聊对应的警信侧 category：1-单聊；2-群聊
        Integer category = Objects.equals(askType, ASK_TYPE_SINGLE) ? CATEGORY_SINGLE : CATEGORY_GROUP;
        reactAiAgentClient.askAI(buildAIReq(imUser, content, agentId, askType, sourceMsg, ctx), detail -> {
            try {
                String msg;
                if (detail.endsWith(END_FLAG)) {
                    msg = detail.replace(END_FLAG, "");
                    // 擦除告警
                    clearAlarm();
                } else if (detail.endsWith(ERROR_FLAG)) {
                    msg = "系统错误";
                    // 上报告警
                    reportAlarm();
                } else {
                    logger.warn("meet strange resp:{}", detail);
                    msg = "系统错误";
                    // 上报告警
                    reportAlarm();
                }
                sendMsg(virtualUserAppId, proxyUserId, msg, to, srcMsgId, category);
                sendToCAgent(msg);
            } catch (Exception e) {
                logger.error("Group AI handle error", e);
                sendMsg(virtualUserAppId, proxyUserId, "系统错误", to, srcMsgId, category);
            }
        });
    }

    /**
     * 调用 AI Agent 但不回复给 IM（用于群聊普通消息推送场景）
     */
    private void askAiNoReply(ImUser imUser, String content, Long agentId, Integer askType,
                              IMOfflineMsgItemVo sourceMsg, MsgContext ctx) {
        reactAiAgentClient.askAINoReply(buildAIReq(imUser, content, agentId, askType, sourceMsg, ctx));
    }

    private AskAIReq buildAIReq(ImUser imUser, String content, Long agentId, Integer askType,
                                IMOfflineMsgItemVo sourceMsg, MsgContext ctx) {
        var imDepartment = imUser.getPrimaryDepartment();
        imDepartment = imDepartment == null ? new ImUser.UserDepartment() : imDepartment;
        AskAIReq req = new AskAIReq();
        req.setUserName(imUser.getName());
        req.setUserID(imUser.getIdCard());
        req.setDepartmentCode(imDepartment.getDepartmentCode());
        req.setDepartmentId(imDepartment.getId() != null ? imDepartment.getId() + "" : null);
        req.setDepartmentName(imDepartment.getDepartmentName());
        req.setContent(content);
        req.setAgent(agentId);
        req.setAskType(askType);
        req.setImSessionId(resolveImSessionId(sourceMsg, askType));
        req.setImExtra(buildIMMsgToAIReq(content, sourceMsg, ctx, askType, imUser));
        return req;
    }

    /**
     * 兼容旧 @ 模式（processAtGroupAIMsg），不传 imExtra
     */
    private AskAIReq buildAIReq(ImUser imUser, String content, Long agentId, Integer askType) {
        var imDepartment = imUser.getPrimaryDepartment();
        imDepartment = imDepartment == null ? new ImUser.UserDepartment() : imDepartment;
        AskAIReq req = new AskAIReq();
        req.setUserName(imUser.getName());
        req.setUserID(imUser.getIdCard());
        req.setDepartmentCode(imDepartment.getDepartmentCode());
        req.setDepartmentId(imDepartment.getId() + "");
        req.setDepartmentName(imDepartment.getDepartmentName());
        req.setContent(content);
        req.setAgent(agentId);
        req.setAskType(askType);
        return req;
    }

    /**
     * 解析 IM 会话 ID
     * 单聊：用 from（对方用户 ID）标识会话
     * 群聊：用 to（群 ID）标识会话
     */
    private Long resolveImSessionId(IMOfflineMsgItemVo sourceMsg, Integer askType) {
        String idStr = Objects.equals(askType, ASK_TYPE_SINGLE) ? sourceMsg.getFrom() : sourceMsg.getTo();
        try {
            return idStr != null ? Long.parseLong(idStr) : null;
        } catch (NumberFormatException e) {
            logger.warn("parse imSessionId failed, idStr:{}", idStr);
            return null;
        }
    }

    /**
     * 构建 IMMsgToAIReq，作为 imExtra 传给 agent 侧
     * agent 侧有 imExtra 时直接作为请求 body 发送
     * 复用 MsgContext 中已解析的图片和群信息，避免重复解析和查询
     */
    private Map<String, Object> buildIMMsgToAIReq(String content, IMOfflineMsgItemVo sourceMsg,
                                                   MsgContext ctx, Integer askType, ImUser imUser) {
        // 协同岗场景：fromRealUserId 有值时，from 是协同岗虚拟ID，user 需要用协同岗名称和ID
        String user;
        if (StringUtils.isNotBlank(sourceMsg.getFromRealUserId())) {
            user = resolveCoopPostUser(sourceMsg, ctx);
        } else {
            user = imUser.getName() + "(" + imUser.getId() + ")";
        }
        IMMsgToAIReq.IMMsgToAIReqBuilder builder = IMMsgToAIReq.builder()
                .query(content)
                .user(user)
                .id_card(imUser.getIdCard())
                .client_type(IMMsgToAIReq.CLIENT_TYPE_5110);

        if (Objects.equals(askType, ASK_TYPE_SINGLE)) {
            builder.chat_source(IMMsgToAIReq.CHAT_SOURCE_SINGLE);
            builder.at_xq(false);
            builder.session_id(sourceMsg.getFrom());
        } else {
            builder.chat_source(IMMsgToAIReq.CHAT_SOURCE_GROUP);
            builder.session_id(sourceMsg.getTo());
            builder.at_xq(ctx.atQuestion != null);
            if (ctx.groupVo != null) {
                builder.group_name(StringUtils.isNotBlank(ctx.groupVo.getName()) ? ctx.groupVo.getName()
                        : ctx.groupVo.getUndefinedName());
                builder.group_description(StringUtils.isNotBlank(ctx.groupVo.getIntroduction())
                        ? ctx.groupVo.getIntroduction() : "");
            }
        }

        // 复用 ctx 中已解析的图片信息和已下载的字节数据
        IMOfflineMsgItemVo.MMSMsgVo img = ctx.imageMsg != null ? ctx.imageMsg : ctx.quotedImage;
        if (img != null) {
            boolean hasText = StringUtils.isNotBlank(content);
            builder.message_type(hasText
                    ? IMMsgToAIReq.MESSAGE_TYPE_TEXT_IMAGE : IMMsgToAIReq.MESSAGE_TYPE_IMAGE);
            builder.images(Collections.singletonList(
                    IMMsgToAIReq.ImageItem.builder()
                            .image_id(img.getFileKey())
                            .source_type(IMMsgToAIReq.ImageItem.SOURCE_TYPE_BASE64)
                            .data(ctx.imageBytes != null ? Base64.getEncoder().encodeToString(ctx.imageBytes) : null)
                            .mime_type(resolveMimeType(img.getFileName(), ctx.imageBytes))
                            .size(Math.toIntExact(img.getFileSize()))
                            .build()
            ));
        } else {
            builder.message_type(IMMsgToAIReq.MESSAGE_TYPE_TEXT);
            builder.images(Collections.emptyList());
        }

        return JsonUtil.convert(builder.build(), Map.class);
    }

    public void sendMsg(String userId, Long proxyUserId, String text, String to, String srcMsgId, Integer category) {
        ImMessageRequest<TxtMsgVo> request = new ImMessageRequest<>();

        request.setCategory(category);
        request.setMsgType(1);
        request.setPlaintext(1);
        request.setFrom(proxyUserId);
        request.setTo(to);

        TxtMsgVo txtVo = new TxtMsgVo();
        txtVo.setText(text);
        txtVo.setSrcMsgId(srcMsgId);

        request.setMsg(txtVo);
        groupAiClient.sendMsg(userId, request);
    }

    private void sendToCAgent(String text) {
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage("AT_GROUP_AI").broadcast()
                .body("AT_GROUP_AI", "RESPONSE", text).build();
        streamBridge.send("cloudcmd-cagent", cagentMqFrame);
    }
}