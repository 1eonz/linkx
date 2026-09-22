package com.tdtech.cloudcmd.cagent.service.inbound;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.cagent.server.MsgHandler;
import com.tdtech.cloudcmd.cagent.server.component.MsgContext;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrameHeader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SubscribeMessageHandler implements MsgHandler {
    @Resource
    private ObjectMapper mapper;

    @Override
    public boolean shouldHandle(CdcFrame msg) {
        return CdcFrameHeader.TYPE_HANDSHAKE_SUBSCRIBE == msg.getHeader().getType();
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public void onMessage(MsgContext ctx, CdcFrame msg) {
        log.info("on subscribe event:{}", msg);
        CdcFrameHeader cdcFrameHeader = new CdcFrameHeader(CdcFrameHeader.TYPE_HANDSHAKE_SUBSCRIBE_SUCCESS);
        Map<String, Object> map = new HashMap<>();
        map.put("notify_type", "subscribeStatus");
        map.put("type", CdcFrameHeader.TYPE_HANDSHAKE_SUBSCRIBE_SUCCESS);
        CdcFrame subscribeResult = new CdcFrame(cdcFrameHeader, mapper.writeValueAsString(map));
        ctx.write(subscribeResult);
    }
}
