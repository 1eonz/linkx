package com.tdtech.cloudcmd.icp.proxy.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.icp.proxy.ws.client.WebSocketMessageHandler;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.CmdTypeEnum;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.FieldNameEnum;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@ChannelHandler.Sharable
public class ObserverbleWebSocketMessageHandler extends WebSocketMessageHandler {
    public final ConcurrentHashMap<CmdTypeEnum, Map<Long, Consumer<JsonNode>>> handlerMap = new ConcurrentHashMap<>();

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    @Qualifier("asyncMessageTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @SneakyThrows
    @Override
    public void onTextMessage(TextWebSocketFrame msg) {
        var payload = msg.text();
        JsonNode jsonNode = objectMapper.readTree(payload);
        String cmd = jsonNode.get(FieldNameEnum.CMD.getValue()).textValue();
        CmdTypeEnum cmdTypeEnum = CmdTypeEnum.cmdOf(cmd);
        if (cmdTypeEnum == null) {
            log.debug("unknown cmd type:{} payload:{}", cmd, payload);
            return;
        }
        if (!handlerMap.containsKey(cmdTypeEnum) || handlerMap.get(cmdTypeEnum).isEmpty()) {
            log.debug("no handler for cmdtype:{} payload:{}", cmd, payload);
            return;
        }
        for (var stringConsumer : handlerMap.get(cmdTypeEnum).values()) {
            try {
                stringConsumer.accept(jsonNode);
            } catch (Exception e) {
                log.error("consume error:{}", JsonUtil.toJsonStr(jsonNode), e);
            }
        }
    }

    public void register(CmdTypeEnum type, Long id, Consumer<JsonNode> consumer) {
        handlerMap.computeIfAbsent(type, k -> new HashMap<>()).put(id, consumer);
        log.debug("register:{} {} ", type, id);
    }

    public void unregister(CmdTypeEnum type, Long id) {
        var map = handlerMap.get(type);
        if (map != null) {
            map.remove(id);
            log.debug("remove register:{} {} {}", type, id, handlerMap);
        }
    }

}
