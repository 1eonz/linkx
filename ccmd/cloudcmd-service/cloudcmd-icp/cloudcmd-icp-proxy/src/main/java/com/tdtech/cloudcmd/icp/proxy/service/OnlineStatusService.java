package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.entity.OnlineStatus;
import com.tdtech.cloudcmd.icp.proxy.repo.OnlineStatusMapper;
import com.tdtech.cloudcmd.icp.proxy.util.HashUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.ResourceNotify;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OnlineStatusService {

    private final OnlineStatusMapper onlineStatusMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final GisService gisService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;

    public void doSub(List<String> isdns) {
        if (isdns == null || isdns.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(isdns, 200);
        for (var value : group.values()) {
            try {
                icpHttpClient.statusSub(authService.getSessionStr(), value);
            } catch (Exception e) {
                log.error("sub online status failed:{}", value, e);
            }
        }
    }

    public void handleWsNotify(JsonNode jsonNode) {
        // normalize value
        ResourceNotify sdkNtf = JsonUtil.convert(jsonNode, ResourceNotify.class);
        if (sdkNtf == null) {
            log.warn("empty notify");
            return;
        }
        var data = sdkNtf.normalizeToStatus();
        if (data == null || data.isEmpty()) {
            log.warn("empty notify");
            return;
        }
        // de-duplication to last value with isdn and group with status type
        var result = data.stream().filter(a -> {
            if (a.getIsdn() == null) {
                return false;
            } else if (!Objects.equals(a.getStatusType(), "15") && !Objects.equals(a.getStatusType(), "16")) {
                return false;
            } else {
                return Objects.equals(a.getStatusValue(), "4011") || Objects.equals(a.getStatusValue(), "4012");
            }
        }).collect(Collectors.groupingBy(a -> HashUtil.hash(a.getIsdn()) % 4, Collectors.toList()));
        if (result.isEmpty()) {
            return;
        }
        result.forEach((p, l) -> streamBridge.send("icp-online-status",
            MessageBuilder.withPayload(l).setHeader("partitionKey", p).build()));
    }

    @Bean("onIcpOnlineStatus")
    public Consumer<List<List<ResourceNotify.Status>>> onIcpOnlineStatus() {
        return msg -> {
            try {
                log.info("consume online status message:{}", msg);
                var collect = msg.stream().flatMap(Collection::stream).collect(
                    Collectors.toMap(ResourceNotify.Status::getIsdn, Function.identity(), (o, n) -> n,
                        LinkedHashMap::new));
                log.info("on status:{}", collect.values());
                var onlineStatus = BeanCopyUtils.copyList(collect.values(), OnlineStatus::new);
                
                // 查询数据库中已存在的记录，用于保留ID
                Map<String, Long> existingIdMap = new HashMap<>();
                List<String> isdns = onlineStatus.stream()
                    .map(OnlineStatus::getIsdn)
                    .filter(isdn -> isdn != null && !isdn.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
                if (!isdns.isEmpty()) {
                    List<OnlineStatus> existingRecords = onlineStatusMapper.selectList(
                        Wrappers.lambdaQuery(OnlineStatus.class)
                            .in(OnlineStatus::getIsdn, isdns)
                            .select(OnlineStatus::getId, OnlineStatus::getIsdn)
                    );
                    existingRecords.forEach(record -> 
                        existingIdMap.put(record.getIsdn(), record.getId())
                    );
                }
                
                for (var status : onlineStatus) {
                    // 如果数据库中已存在相同isdn的记录，保留原ID，否则生成新ID
                    Long existingId = existingIdMap.get(status.getIsdn());
                    status.setId(existingId != null ? existingId : idWorker.nextId());
                }
                // 批量新增或更新
                onlineStatusMapper.insertOrUpdateBatch(onlineStatus);
                publishCameraLevelMsg(onlineStatus);
                var onLines =
                    onlineStatus.stream().filter(a -> Objects.equals(a.getStatusValue(), "4011")).map(a -> a.getIsdn())
                        .collect(Collectors.toList());
                gisService.doSub(onLines);
            } catch (Exception e) {
                log.error("consume online status error:{}", msg, e);
            }
        };
    }

    private void publishCameraLevelMsg(Object payload) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_ONLINE_STATUS").multicast()
            .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build()
            .body("ICP_ONLINE_STATUS", "ICP_ONLINE_STATUS", payload).build();
        streamBridge.send("cloudcmd-cagent", build);
    }
}