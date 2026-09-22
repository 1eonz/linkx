package com.tdtech.cloudcmd.im.jingxin.client;

import com.tdtech.cloudcmd.im.jingxin.client.entity.ImHeadersReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.SubscribeReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsAck;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsResponse;
import com.tdtech.cloudcmd.im.jingxin.client.utils.RandomUtil;
import com.tdtech.cloudcmd.im.jingxin.client.ws.WebSocketMessageHandler;
import com.tdtech.cloudcmd.im.jingxin.client.ws.WsClient;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.advice.SystemException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.cloud.stream.function.StreamBridge;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ImWsClient implements NamedLogger {


    @Getter
    private final String name;
    private final CachedImConfig cachedImConfig;
    private final ImHttpClient imHttpClient;
    private final StreamBridge streamBridge;
    private final WsClient wsClient;

    @Resource
    private ReportUtil reportUtil;


    public ImWsClient(String name, CachedImConfig cachedImConfig, ImHttpClient imHttpClient,
        StreamBridge streamBridge) {
        this.name = name;
        this.cachedImConfig = cachedImConfig;
        this.imHttpClient = imHttpClient;
        this.streamBridge = streamBridge;
        this.wsClient = new WsClient(this.name, this.new ImWebSocketMessageHandler());
    }

    public void keepAlive() {
        if (!wsClient.isActive()) {
            log("warn", "channel not active, connect now...");
            var clientConfigGroup = imHttpClient.getClientConfigGroup();
            var wsHost = cachedImConfig.getConfig(clientConfigGroup.getWsHostConfKey());
            String cliId = cachedImConfig.getConfig(clientConfigGroup.getCliIdConfKey());
            String uscc = cachedImConfig.getConfig(clientConfigGroup.getHeadersUsccKey());
            String allowSeid = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSeidKey());
            String allowSekey = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSekeyKey());
            ImHeadersReq imHeadersReq = new ImHeadersReq();
            imHeadersReq.setUscc(uscc);
            imHeadersReq.setAllowSeid(allowSeid);
            imHeadersReq.setAllowSekey(allowSekey);
            imHeadersReq.setXClientId(cliId);
            try {
                var uri = new URI(wsHost);
                var token = imHttpClient.getToken();
                if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
                    String errorMsg = "keepAlive auth failed";
                    log("warn", errorMsg);
                    return;
                }
                wsClient.connect(uri, token, imHeadersReq, RandomUtil.randomString());
                // 只有name为coopImWsClient才订阅 群ai和一比14亿不用订阅这三个通知
                log("info", this.name + "开始订阅");
                if("coopImWsClient".equals(this.name)){
                    // 订阅人员状态变更
                    subscribeUserStateNoticeMessage();
                    // 订阅组织部门变更
                    subscribeDepartmentNoticeMessage();
                    // 订阅群组变更
                    subscribeGroupNoticeMessage();
                    // 删除redis消息推送目标时间
                    imHttpClient.clearSendMessageTargetTime();
                }
                // 连接成功，则上报MSIP，擦除活动告警
                clearAlarm();
            } catch (WsClient.HandshakeException e) {
                if (Objects.equals(e.getStatus(), HttpResponseStatus.UNAUTHORIZED)) {
                    imHttpClient.deprecateToken();
                }
                log("error", "connect error:{} {} {}", wsHost, imHeadersReq, e);
                // 上报与警信服务器连接失败的告警
                reportAlarm(e.getMessage());
            } catch (Exception e) {
                log("error", "connect error:{} {} {}", wsHost, imHeadersReq, e);
                // 上报与警信服务器连接失败的告警
                reportAlarm(e.getMessage());
            }
        }
    }

    public void subscribeUserStateNoticeMessage() {
        String signFlag = cachedImConfig.getConfig("FEATURE_COMPATIBILITY_SIGN");
        boolean isClosed = StringUtils.isBlank(signFlag) || (!"0".equals(signFlag));
        if (isClosed) {
            log("info", "subscribeUserStateNoticeMessage isClosed");
            return;
        }
        List<SubscribeReq> requestList = Arrays.asList(new SubscribeReq("addressbook", "userStateNotice"));
        imHttpClient.subscribeMessage(requestList);
    }

    public void subscribeGroupNoticeMessage() {
        List<SubscribeReq> requestList = Arrays.asList(new SubscribeReq("group", "groupNotice"));
        imHttpClient.subscribeMessage(requestList);
    }
    public void subscribeDepartmentNoticeMessage() {
        String signFlag = cachedImConfig.getConfig("DEPARTMENT_SYNC_SIGN");
        boolean isClosed = StringUtils.isBlank(signFlag) || (!"true".equals(signFlag));
        if (isClosed) {
            log("info", "subscribeDepartmentNoticeMessage isClosed");
            return;
        }
        List<SubscribeReq> requestList = Arrays.asList(new SubscribeReq("addressbook", "departmentNotice"));
        imHttpClient.subscribeMessage(requestList);
    }

    private void reportAlarm(String param) {
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED, param);
    }

    private void clearAlarm() {
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED);
    }

    @SneakyThrows
    public void close() {
        if (wsClient.isActive()) {
            wsClient.close();
        }
    }

    @ChannelHandler.Sharable
    public class ImWebSocketMessageHandler extends WebSocketMessageHandler {

        @Override
        public void onTextMessage(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame msg) {
            var text = msg.text();
            var wsResponse = JsonUtil.parseJson(text, WsResponse.class);
            try {
                if (wsResponse == null) {
                    throw new SystemException("parse msg error:" + text);
                }
                if (Objects.equals(wsResponse.getNotifyType(), "pong")) {
                    log("info", "pong:{}", text);
                    return;
                }
                if (wsResponse.getData() == null) {
                    log("warn", "empty data:{}", text);
                    return;
                }
                log("info", "receive message:{}", text);
                streamBridge.send("cloudcmd-im-jingxin-ws-" + name, text);
                if (wsResponse.getNeedAck() != null && wsResponse.getNeedAck()) {
                    var wsAck = new WsAck(wsResponse);
                    channelHandlerContext.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJsonStr(wsAck)));
                }
            } catch (Exception e) {
                if (wsResponse != null && wsResponse.getNeedAck() != null && wsResponse.getNeedAck()) {
                    var wsAck = new WsAck(wsResponse);
                    wsAck.setCode(1);
                    wsAck.setMsg(e.getMessage());
                    channelHandlerContext.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJsonStr(wsAck)));
                }
                log("error", "error", e);
            }
        }

        @Override
        public void onHandShakeFinished(ChannelHandlerContext channelHandlerContext) {
            // nope
        }
    }
}
