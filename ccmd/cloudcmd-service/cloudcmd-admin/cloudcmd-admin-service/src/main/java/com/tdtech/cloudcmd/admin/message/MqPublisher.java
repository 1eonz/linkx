package com.tdtech.cloudcmd.admin.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;
import com.tdtech.cloudcmd.admin.resource.entity.IcpConfig;
import com.tdtech.cloudcmd.bean.MsgBody;
import com.tdtech.cloudcmd.util.json.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lwx623661
 */
@Slf4j
@Component
@EnableBinding(Publisher.class)
public class MqPublisher {

    private static final String global = "GLOBAL";
    @Autowired
    private Publisher publisher;

    public boolean sendGlobalMessage(String notifyType, Globals data) {
        MsgBody<Globals> msgBody = new MsgBody<>();
        msgBody.setData(data);
        msgBody.setModule(global);
        msgBody.setNotifyType(notifyType);
        log.info("publish admin data is:{}", msgBody);
        publisher.globalConfigChange().send(MessageBuilder.withPayload(JsonUtil.toJsonStr(data)).build());
        return publisher.globalOutPut().send(MessageBuilder.withPayload(JSONObject.toJSONString(msgBody)).build());
    }

    public void sendIcpConfigMessage(IcpConfig data) {
        log.info("publish icp config data is:{}", data);
        publisher.icpConfigChange().send(MessageBuilder.withPayload(JsonUtil.toJsonStr(data)).build());
    }
}
