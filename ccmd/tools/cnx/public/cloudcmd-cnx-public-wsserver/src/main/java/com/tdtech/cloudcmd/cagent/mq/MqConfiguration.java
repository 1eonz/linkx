package com.tdtech.cloudcmd.cagent.mq;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;
import com.tdtech.cloudcmd.cagent.service.MessageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MqConfiguration {
    @Resource
    private ObjectMapper mapper;
    @Resource
    private MessageService messageService;
    @Resource
    private ExecutorService executorService;

    @Bean
    public Consumer<List<CagentMqFrame>> onMessage() {
        return msgs -> {
            if (msgs == null || msgs.isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("on mesasge:{}", Arrays.toString(msgs.toArray()));
            }
            var completableFutures = new CompletableFuture[msgs.size()];
            for (int i = 0; i < msgs.size(); i++) {
                var frame = msgs.get(i);
                completableFutures[i] = CompletableFuture.supplyAsync(() -> {
                    try {
                        if (frame == null) {
                            return "done";
                        }
                        CdcFrameHeader header =
                            new CdcFrameHeader(frame.getType(), frame.getTokenkey(), frame.getSubsystem());
                        header.setSendStrategy(frame.getSendStrategy());
                        String body = mapper.writeValueAsString(frame.getBody());
                        CdcFrame cdcframe = new CdcFrame(header, body, frame.getPrivOrg());
                        messageService.dispatch(cdcframe);
                        return "done";
                    } catch (Exception e) {
                        log.error("json error", e);
                        return "error";
                    }
                }, executorService);
            }
            var join = CompletableFuture.allOf(completableFutures);
            join.join();
            if (join.isCompletedExceptionally()) {
                throw new RuntimeException("task error");
            }
        };
    }

    @Bean
    public Consumer<String> logout() {
        return s -> {
            try {
                log.debug("on logout:{}", s);
                CdcFrameHeader header = new CdcFrameHeader(CdcFrameHeader.TYPE_PASSPORT_KICKOUT, s, "");
                String s1 = mapper.writeValueAsString(Collections.singletonMap("reason", "relogin"));
                CdcFrame data = new CdcFrame(header, s1);
                messageService.dispatch(data);
            } catch (Exception e) {
                log.error("json error", e);
            }
        };
    }

    @Bean
    public Consumer<String> message() {
        return s -> {
            log.debug("on message message:{}", s);
            CdcFrameHeader header = new CdcFrameHeader((short)2);
            header.setToken("*-*");
            header.setSubsystem("MS");
            JSONObject bodyJSONObject = new JSONObject();
            JSONObject msgJsonObject = JSONObject.parseObject(s);
            String notifyType = msgJsonObject.getString("notifyType");
            bodyJSONObject.put("module", "MS");
            bodyJSONObject.put("notifyType", notifyType);
            bodyJSONObject.put("data", msgJsonObject);
            String bodyData = bodyJSONObject.toJSONString();
            CdcFrame cdcframe = new CdcFrame(header, bodyData);
            messageService.dispatch(cdcframe);
        };
    }

    @Bean
    public Consumer<String> alert() {
        return s -> {
            log.debug("on alert:{}", s);
            JSONObject msgJsonObject = JSONObject.parseObject(s);
            CdcFrameHeader header = new CdcFrameHeader((short)2);
            header.setToken("*-*");
            header.setSubsystem(msgJsonObject.getString("module"));
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("module", msgJsonObject.getString("module"));
            bodyJson.put("notifyType", "sysAlertMessage");
            bodyJson.put("data", msgJsonObject);
            String bodyData = bodyJson.toJSONString();
            CdcFrame cdcframe = new CdcFrame(header, bodyData);
            messageService.dispatch(cdcframe);
        };
    }

    @Bean
    public Consumer<String> admin() {
        return s -> {
            log.debug("on admin:{}", s);
            JSONObject msgJsonObject = JSONObject.parseObject(s);
            CdcFrameHeader header = new CdcFrameHeader((short)2);
            String token =
                    Optional.ofNullable(msgJsonObject.getString("tokenkey")).orElse("*-*");
            header.setToken(token);
            header.setSubsystem(msgJsonObject.getString("module"));
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("module", msgJsonObject.getString("module"));
            bodyJson.put("notifyType", msgJsonObject.getString("notifyType"));
            bodyJson.put("data", msgJsonObject.get("data"));
            String bodyData = bodyJson.toJSONString();
            CdcFrame cdcframe = new CdcFrame(header, bodyData);
            messageService.dispatch(cdcframe);
        };
    }
}
