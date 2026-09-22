package com.tdtech.cloudcmd.im.jingxin.client.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ai-agent 核查记录同步客户端（同步阻塞调用）。
 * <p>
 * 供 dashboard 统计同步定时任务按双游标拉取人员核查记录，host 从 Globals 全局配置
 * {@code GROUP_AI_HOST} 获取（含协议+端口），与 {@code ReactAiAgentClient} 同源，
 * 由 {@link CachedImConfig} 提供 10s 缓存。
 * <p>
 * HTTP 底层复用 {@link HttpClient}（cloudcmd-spring-boot-starter-web 提供），
 * 与 {@code ImHttpClient} 同款，避免在 im-client 引入 Spring RestTemplate 依赖。
 */
@Slf4j
@RequiredArgsConstructor
public class AiAgentRecordClient {

    /** Globals 全局配置 key：AI Agent 服务地址（含协议+端口），与 ReactAiAgentClient 取同一配置。 */
    private static final String GROUP_AI_HOST = "GROUP_AI_HOST";

    /**
     * ai-agent 核查记录游标接口路径（与 ai-agent 服务侧 AiAgentConfigController 的 @RequestMapping 对齐）。
     * 完整 URL = GROUP_AI_HOST（补尾斜杠） + 此路径。
     */
    private static final String RECORD_CURSOR_PATH = "proxy/ai/v1/aiagent/management/record/cursor";

    /** ai-agent 响应成功码（与 ReactAiAgentClient 的 AIResponse 判断一致）。 */
    private static final int CODE_SUCCESS = 0;

    private final CachedImConfig cachedImConfig;

    private final HttpClient httpClient;

    /**
     * 按 (time, id) 双游标拉取核查记录。
     *
     * @param agentConfigId 智能体配置 id，-1 表示人员核查记录
     * @param timeAfter     游标时间（上次最后一条的 time），首次传 null
     * @param idAfter       游标 id（上次最后一条的 id），首次传 null
     * @param size          每页大小
     * @return 游标分页结果；host 未配置、调用异常、响应非成功时返回 null
     */
    public AgentRecordCursorResult listRecordByCursor(Long agentConfigId, Date timeAfter, Long idAfter, int size) {
        String host = cachedImConfig.getConfig(GROUP_AI_HOST);
        if (StringUtils.isBlank(host)) {
            log.warn("AiAgentRecordClient GROUP_AI_HOST is blank");
            return null;
        }
        host = host.trim();
        if (!host.endsWith("/")) {
            host = host + "/";
        }
        URI uri = buildUri(host + RECORD_CURSOR_PATH, agentConfigId, timeAfter, idAfter, size);
        try {
            AiAgentResponse<AgentRecordCursorResult> resp = httpClient.getJson(uri, null,
                new TypeReference<AiAgentResponse<AgentRecordCursorResult>>() {});
            if (resp == null || !Objects.equals(resp.getCode(), CODE_SUCCESS) || resp.getData() == null) {
                log.warn("AiAgentRecordClient non-success, uri:{}, resp:{}", uri, resp);
                return null;
            }
            return resp.getData();
        } catch (Exception e) {
            log.error("AiAgentRecordClient error, uri:{}", uri, e);
            return null;
        }
    }

    private URI buildUri(String url, Long agentConfigId, Date timeAfter, Long idAfter, int size) {
        Map<String, String> params = new HashMap<>();
        params.put("agentConfigId", String.valueOf(agentConfigId));
        if (timeAfter != null) {
            LocalDateTime ldt = LocalDateTime.ofInstant(timeAfter.toInstant(), ZoneId.systemDefault());
            params.put("timeAfter", ldt.toString());
        }
        if (idAfter != null) {
            params.put("idAfter", String.valueOf(idAfter));
        }
        params.put("size", String.valueOf(size));
        return HttpClient.buildUri(url, params);
    }
}