package com.tdtech.cloudcmd.im.jingxin.server.chatmessagehandler;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.IMOfflineMsgItemVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImMessageRequest;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.TxtMsgVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsResponse;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsTextMessage;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.React1Over1p4BClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BCallReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.util.FaceUtil;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplate;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class IM1o1p4BMessageHandler {

    @Resource
    @Qualifier("oneO1p4BImHttpClient")
    private ImHttpClient imHttpClient;
    @Resource
    private LabelService labelService;
    @Resource
    private React1Over1p4BClient react1Over1p4BClient;
    @Resource
    private CollaborationPostService collaborationPostService;
    @Resource
    private  FaceUtil faceUtil;
    @Resource
    private ReportUtil reportUtil;

    @Bean("im1p4BMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                log.info("im1p4BMessage on message: {}", wsResponse);
                if (!Objects.equals(wsResponse.getModule(), "im") || !Objects.equals(wsResponse.getNotifyType(),
                    "TEXT_MSG")) {
                    return;
                }
                var wsTextMessage = JsonUtil.convert(wsResponse.getData(), WsTextMessage.class);
                if (!isAIMsg(wsTextMessage)) {
                    return;
                }
                var post = collaborationPostService.getById(wsTextMessage.getFrom());
                if (post == null) {
                    log.warn("post not found:{}", wsTextMessage.getFrom());
                    sendMsg("ERR：只有1：14亿协同岗才能发起问询", wsTextMessage);
                    return;
                }
                var imUsers = imHttpClient.userPage(null, wsTextMessage.getFromRealUserId() + "").getResults();
                if (imUsers == null || imUsers.isEmpty()) {
                    log.warn("im user not found:{}", wsTextMessage.getFrom());
                    sendMsg("ERR：没有找到问询发起人", wsTextMessage);
                    return;
                }
                var imUser = imUsers.get(0);
                var imDepartment = imUser.getPrimaryDepartment();
                imDepartment = imDepartment == null ? new ImUser.UserDepartment() : imDepartment;
                var groupId = wsTextMessage.getTo();
                var groupVo = imHttpClient.queryGroupDetail(Long.parseLong(groupId));
                if (groupVo == null) {
                    log.warn("group not found:{}", wsTextMessage.getFrom());
                    sendMsg("ERR：获取群组信息异常", wsTextMessage);
                    return;
                }

                var picture = getPicture(wsTextMessage.getMsg().getSrcMsgId());
                if (picture == null) {
                    log.warn("pic not found:{}", wsTextMessage.getMsg().getSrcMsgId());
                    sendMsg("ERR：没有获取到图片", wsTextMessage);
                    return;
                }
                try {
                    picture = faceUtil.pickFirstFace(picture);
                } catch (Exception e) {
                    log.error("pic process failed:{}", wsTextMessage.getMsg().getSrcMsgId(), e);
                    sendMsg("ERR：图像预处理失败", wsTextMessage);
                    return;
                }
                if (picture == null || picture.length == 0) {
                    log.warn("pic process failed:{}", wsTextMessage.getMsg().getSrcMsgId());
                    sendMsg("ERR：没有识别到人脸", wsTextMessage);
                    return;
                }
                var department = imHttpClient.getToken().getDepartment();
                var req = new OneOver1p4BCallReq()//
                    .setGroupId(groupId)//
                    .setGroupName(
                        groupVo.getName() == null || groupVo.getName().isBlank() ? "default" : groupVo.getName())//
                    .setAreaCode(department == null || department.getDepartmentCode() == null ? "0"
                        : department.getDepartmentCode())//
                    .setGroupType(groupVo.getType())//
                    .setImage(Base64.encodeBase64String(picture))//
                    .setOperatorInfo(new OneOver1p4BCallReq.Operator(imUser.getName(), imUser.getIdCard(),
                        imDepartment.getDepartmentCode(), imDepartment.getId() + "", imDepartment.getDepartmentName()));
                react1Over1p4BClient.doAskAsync(req, resp -> {
                    sendMsg("姓名：" + resp.getName() + "\n 身份证号：" + resp.getIdCard(), wsTextMessage);
                    // 擦除告警
                    clearAlarm(AlarmTemplateZhEnum.INVOKING_PERSONNEL_CHECK_FAILED);
                }, (str) -> {
                    sendMsg(str, wsTextMessage);
                    // 上报告警
                    reportAlarm(AlarmTemplateZhEnum.INVOKING_PERSONNEL_CHECK_FAILED, imUser.getName(), "图片");
                });
            } catch (Exception e) {
                log.error("im1p4BMessage consume error", e);
            }
        };
    }

    private void reportAlarm(AlarmTemplate alarmTemplate, Object... params) {
        reportUtil.saveAlarm2MSIP(alarmTemplate, params);
    }

    private void clearAlarm(AlarmTemplate alarmTemplate) {
        reportUtil.clearAlarm2MSIP(alarmTemplate);
    }

    private void sendMsg(String text, WsTextMessage wsTextMessage) {
        ImMessageRequest<TxtMsgVo> request = new ImMessageRequest<>();
        request.setCategory(2);
        request.setMsgType(1);
        request.setPlaintext(1);
        request.setFrom(imHttpClient.getToken().getProxyUser().getId());
        request.setTo(wsTextMessage.getTo());
        TxtMsgVo txtVo = new TxtMsgVo();
        txtVo.setText(text);
        txtVo.setSrcMsgId(wsTextMessage.getMsg().getSrcMsgId() + "");
        request.setMsg(txtVo);
        imHttpClient.sendMsg(request);
    }

    private byte[] getPicture(Long picMsgId) {
        var imOfflineMsgPageVo = imHttpClient.offlineMsg(picMsgId);
        if (imOfflineMsgPageVo == null || imOfflineMsgPageVo.getImMsgs().isEmpty()) {
            log.warn("img msg empty:{}", picMsgId);
            return null;
        }
        var imOfflineMsgVo = imOfflineMsgPageVo.getImMsgs().get(0);
        var offlineMsgItem = imOfflineMsgVo.getData();
        if (!Objects.equals(imOfflineMsgVo.getNotifyType(), "MEDIA_MSG") || offlineMsgItem == null || !Objects.equals(
            offlineMsgItem.getMsgType(), 2)) {
            log.warn("img msg {} is not a media msg", picMsgId);
            return null;
        }
        var msg = offlineMsgItem.getMsg();
        var imageMsg = JsonUtil.convert(msg, IMOfflineMsgItemVo.MMSMsgVo.class);
        if (!Objects.equals(imageMsg.getFileType(), 1)) {
            log.warn("img msg {} is not a media msg", picMsgId);
            return null;
        }
        return imHttpClient.downloadIcon(imageMsg.getFileKey());
    }

    private boolean isAIMsg(WsTextMessage wsTextMessage) {
        Objects.requireNonNull(wsTextMessage);
        Objects.requireNonNull(wsTextMessage.getMsg());
        // check token
        if (imHttpClient.getToken() == null) {
            log.warn("not login yet");
            return false;
        }
        Long proxyUserId = imHttpClient.getToken().getProxyUser().getId();
        if (proxyUserId == null) {
            log.warn("proxy user id is null");
            return false;
        }
        // check msg
        if (wsTextMessage.getMsg().getSrcMsgId() == null) {
            // 不是转发消息
            return false;
        }
        String text = wsTextMessage.getMsg().getText();
        if (text == null || !text.contains("[@" + proxyUserId)) {
            // 不是@1：1.4B的消息
            return false;
        }
        // check post
        var byId = collaborationPostService.getById(wsTextMessage.getFrom());
        return byId != null && byId.getType() == 1;
    }

}
