package com.tdtech.cloudcmd.im.jingxin.server.chatmessagehandler;

import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.ReactAiAgentClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AskAIReq;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ImUserVirtualMapper;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class IMGroupAIMessageHandler {

    private static final String END_FLAG = "[end]";
    private static final String ERROR_FLAG = "[error]";

    private static final Pattern AT_PATTERN = Pattern.compile("\\[@(\\d+):@[^\\]]*?\\]");

    private static final Pattern QUESTION_PATTERN = Pattern.compile(".*\\](.*)$");

    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient coopImHttpClient;

    @Resource
    private GroupAiClient groupAiClient;

    @Resource
    private ReactAiAgentClient reactAiAgentClient;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private StreamBridge streamBridge;

    @Resource
    private ImUserVirtualMapper imUserVirtualMapper;

    private final Parser PARSER = Parser.builder().build();
    private final TextContentRenderer RENDERER = TextContentRenderer.builder().stripNewlines(false) // 需要保留换行可设 true
        .build();

    @Bean("imTextMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                log.info("imTextMessage on message: {}", wsResponse);
                if (!Objects.equals(wsResponse.getModule(), "im") || !Objects.equals(wsResponse.getNotifyType(),
                    "TEXT_MSG")) {
                    return;
                }
                var wsTextMessage = JsonUtil.convert(wsResponse.getData(), WsTextMessage.class);
                String proxyUserId = isAtGroupAIMsg(wsTextMessage);
                log.info("imTextMessage isAtGroupAIMsg: {}", proxyUserId);
                if (proxyUserId != null) {
                    processAtGroupAIMsg(proxyUserId, wsTextMessage);
                }
            } catch (Exception e) {
                log.error("imTextMessage consume error", e);
            }
        };
    }

    private String isAtGroupAIMsg(WsTextMessage wsTextMessage) {
        // 12344234[@26765031121925:@群AI助手]444"
        // "[@26765031121925:@群AI助手]"
        // "[@26765031121925:@群AI助手]abc"
        //String atFlag = String.format("[@%s:@", getProxyUserId());
        //return wsTextMessage.getMsg().getText().contains(atFlag);
        Matcher matcher = AT_PATTERN.matcher(wsTextMessage.getMsg().getText());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String extractQuestion(String raw) {
        Matcher m = QUESTION_PATTERN.matcher(raw.trim());
        return m.find() ? m.group(1).trim() : raw.trim();
    }

    /**
     * 处理@群AI助手消息
     */
    private void processAtGroupAIMsg(String proxyUserId, WsTextMessage sourceMsg) {
        String virtualUserAppId = groupAiClient.getUserByProxyUser(proxyUserId);

        if (virtualUserAppId == null) {
            log.error("can not find proxy user {} and client bind info", proxyUserId);
            return;
        }

        Long proxyUser = Long.parseLong(proxyUserId);

        sendMsg(virtualUserAppId, proxyUser, "AI助手处理中", sourceMsg.getTo(), sourceMsg.getMsgId());

        var text = sourceMsg.getMsg().getText();
        String content = extractQuestion(text);

        log.info("processAtGroupAIMsg content: {}", content);
        if (content.isBlank()) {
            sendMsg(virtualUserAppId, proxyUser, "提问方式为@xx+问题", sourceMsg.getTo(), sourceMsg.getMsgId());
            return;
        }
        // fromRealUserId：主叫真实用户id。仅在category取值为2(群聊消息)，且主叫为协同岗时填写。此时from填写为协同岗虚拟id。
        Long fromRealUserId = sourceMsg.getFromRealUserId();
        Long userId = Objects.nonNull(fromRealUserId) ? fromRealUserId : sourceMsg.getFrom();
        var userInfo = coopImHttpClient.userPage(null, userId + "").getResults();
        if (CollectionUtils.isEmpty(userInfo)) {
            log.info("processAtGroupAIMsg userInfo is null");
            return;
        }
        var imUser = userInfo.get(0);

        var virtualUserInfo = imUserVirtualMapper.selectGroupAiBind(virtualUserAppId);

        if (virtualUserInfo == null) {
            log.error("can not find virtual user {} bind info", virtualUserAppId);
            sendMsg(virtualUserAppId, proxyUser, "AI助手未与智能体绑定，请联系管理员", sourceMsg.getTo(), sourceMsg.getMsgId());
            return;
        }

        reactAiAgentClient.askAI(buildAIReq(imUser, content, virtualUserInfo.getAgentId()), detail -> {
            String msg;
            if (detail.endsWith(END_FLAG)) {
                msg = toPlain(detail.replace(END_FLAG, ""));

                // 擦除告警
                clearAlarm(AlarmTemplateZhEnum.THE_AGENT_IS_DISCONNECTED);
            } else if (detail.endsWith(ERROR_FLAG)) {
                msg = toPlain("系统错误");

                // 上报告警
                reportAlarm(AlarmTemplateZhEnum.THE_AGENT_IS_DISCONNECTED);
            } else {
                log.warn("meet strange resp:{}", detail);
                msg = toPlain("系统错误");

                // 上报告警
                reportAlarm(AlarmTemplateZhEnum.THE_AGENT_IS_DISCONNECTED);
            }
            sendMsg(virtualUserAppId, proxyUser, msg, sourceMsg.getTo(), sourceMsg.getMsgId());
            sendToCAgent(msg);
        });
    }

    private void reportAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.saveAlarm2MSIP(alarmTemplateZhEnum);
    }

    private void clearAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.clearAlarm2MSIP(alarmTemplateZhEnum);
    }

    private AskAIReq buildAIReq(ImUser imUser, String content, Long agentId) {
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
        return req;
    }

    public void sendMsg(String userId, Long proxyUserId, String text, String to, String srcMsgId) {
        ImMessageRequest<TxtMsgVo> request = new ImMessageRequest<>();

        request.setCategory(2);
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

    public String toPlain(String markdown) {
        Node document = PARSER.parse(markdown);
        return RENDERER.render(document);
    }

}
