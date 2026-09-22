package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.entity.Gis;
import com.tdtech.cloudcmd.icp.proxy.repo.GisMapper;
import com.tdtech.cloudcmd.icp.proxy.util.HashUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.Notify;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.FieldNameEnum;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.GisOptTypeEnum;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GisService {

    private final GisMapper gisMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;

    public void doSub(List<String> isdns) {
        if (isdns == null || isdns.isEmpty()) {
            return;
        }
        var group = CollectionUtils.group(isdns, 200);
        for (var value : group.values()) {
            try {
                icpHttpClient.gisSub(authService.getSessionStr(), value);
            } catch (Exception e) {
                log.error("sub gis failed:{}", value, e);
            }
        }
    }

    public void handleWsNotify(JsonNode jsonNode) {
        JsonNode optNode = jsonNode.get(FieldNameEnum.OPT.getValue());
        if (optNode != null) {
            String typeStr = optNode.asText();
            GisOptTypeEnum typeEnum =
                Objects.requireNonNull(GisOptTypeEnum.typeOf(typeStr), "null type enum: " + typeStr);
            switch (typeEnum) {
                case SUB:
                case UNSUB: {
                    log.info("{} gis resp:{}", typeEnum, JsonUtil.toJsonStr(jsonNode));
                    break;
                }
                case REPORT: {
                    var list = parseGis(jsonNode);
                    var result = list.stream().filter(a -> {
                        if (a.getIsdn() == null || a.getIsdn().isBlank()) {
                            return false;
                        } else {
                            return a.getLat() != null && a.getLon() != null;
                        }
                    }).collect(Collectors.groupingBy(a -> HashUtil.hash(a.getIsdn()) % 4, Collectors.toList()));
                    if (result.isEmpty()) {
                        return;
                    }
                    result.forEach((p, l) -> streamBridge.send("icp-gis-status",
                        MessageBuilder.withPayload(l).setHeader("partitionKey", p).build()));
                    break;
                }
                default: {
                    log.warn("meet strange gis message:{}", JsonUtil.toJsonStr(jsonNode));
                }
            }
        }
    }

    @Bean("onIcpGisStatus")
    public Consumer<List<List<Gis>>> onIcpGisStatus() {
        return msg -> {
            try {
                log.info("consume gis status message:{}", msg);
                var collect = msg.stream().flatMap(Collection::stream)
                    .collect(Collectors.toMap(Gis::getIsdn, Function.identity(), (o, n) -> n, LinkedHashMap::new));
                // 批量新增或更新
                gisMapper.insertOrUpdateBatch(collect.values());
                publishGisMsg(collect.values());
            } catch (Exception e) {
                log.error("consume gis status error:{}", msg, e);
            }
        };
    }

    private void publishGisMsg(Collection<Gis> gis) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_GIS").multicast()
            .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build().body("ICP_GIS", "ICP_GIS_NOTIFY", gis)
            .build();
        streamBridge.send("cloudcmd-cagent", build);
    }

    private List<Gis> parseGis(JsonNode message) {
        var gisNotify = JsonUtil.convert(message, new TypeReference<Notify<Gis>>() {
        });
        if (gisNotify == null || gisNotify.getList() == null || gisNotify.getList().isEmpty()) {
            return Collections.emptyList();
        }
        var list = gisNotify.getList();
        var now = new Date();
        
        // 查询数据库中已存在的记录，用于保留ID
        Map<String, Long> existingIdMap = new HashMap<>();
        List<String> isdns = list.stream()
            .map(Gis::getIsdn)
            .filter(isdn -> isdn != null && !isdn.isBlank())
            .distinct()
            .collect(Collectors.toList());
        if (!isdns.isEmpty()) {
            List<Gis> existingRecords = gisMapper.selectList(
                Wrappers.lambdaQuery(Gis.class)
                    .in(Gis::getIsdn, isdns)
                    .select(Gis::getId, Gis::getIsdn)
            );
            existingRecords.forEach(record -> 
                existingIdMap.put(record.getIsdn(), record.getId())
            );
        }
        
        for (var gis : list) {
            // 如果数据库中已存在相同isdn的记录，保留原ID，否则生成新ID
            Long existingId = existingIdMap.get(gis.getIsdn());
            gis.setId(existingId != null ? existingId : idWorker.nextId());
            gis.setCreateTime(now);
            var locationStr = gis.getLocation();
            if (StringUtils.isEmpty(locationStr)) {
                continue;
            }
            var split = locationStr.split(",");
            gis.setLon(new BigDecimal(split[0]));
            gis.setLat(new BigDecimal(split[1]));
            gis.setAlt(new BigDecimal(split[2]));
        }
        return list.stream().filter(a -> a.getLon() != null && a.getLat() != null).collect(Collectors.toList());
    }
}
