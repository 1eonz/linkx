package com.tdtech.cloudcmd.im.jingxin.server.aiclient;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AIRecordReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.AgentRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AIResponse;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AgentConfigVO;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AskAIReq;
import com.tdtech.cloudcmd.util.json.JsonUtil;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * 基于 WebClient 的响应式 AI Agent 客户端 用于向 AI Agent 服务发送异步 HTTP 请求
 */
@Slf4j
@Component
public class ReactAiAgentClient extends BaseReactHttpClient {

    private static final String AI_AGENT_HTTP_HOST_CONF_KEY = "GROUP_AI_HOST";
    private static final String ASK_AI_URL = "proxy/ai/v1/deepseek-zjk/xa/dk/contend";
    private static final String ASK_AI_DETAIL_URL = "proxy/ai/v1/deepseek-zjk/xa/dk/detail";

    private static final String QUERY_RECORD_LIST_URL = "proxy/ai/v1/deepseek-zjk/record/history/list";
    private static final String QUERY_RECORD_COUNT_URL = "proxy/ai/v1/deepseek-zjk/record/history/count";

    private static final String QUERY_AGENT_CONFIG_URL = "proxy/ai/v1/aiagent/management/byid/";

    private final CachedImConfig cachedImConfig;
    private final Scheduler scheduler;

    /**
     * 构造函数，初始化 WebClient 实例 配置默认 Content-Type 为 application/json 设置连接超时为 10 秒
     */
    @SneakyThrows
    public ReactAiAgentClient(CachedImConfig cachedImConfig,
        @Qualifier("groupAITaskExecutorService") ThreadPoolTaskExecutor executorService) {
        super();
        scheduler = Schedulers.fromExecutor(executorService);
        this.cachedImConfig = cachedImConfig;
    }

    private String getGroupAiHost() {
        var host = cachedImConfig.getConfig(AI_AGENT_HTTP_HOST_CONF_KEY);
        if (host == null || host.isBlank()) {
            throw new BusinessException("GROUP_AI_HOST 未配置");
        }
        return host.endsWith("/") ? host : host + "/";
    }

    public Mono<String> poll(String host, String id) {
        var groupAiMaxRetryTimes = cachedImConfig.getConfig("GROUP_AI_MAX_RETRY_TIMES");
        var retry = groupAiMaxRetryTimes!=null&&!groupAiMaxRetryTimes.isBlank()?
            Integer.parseInt(groupAiMaxRetryTimes):45;
        return getJsonAsync(host + ASK_AI_DETAIL_URL + "?id=" + id, null,
            new TypeReference<AIResponse<String>>() {})//
            .map(aiResp -> {
                Integer code = aiResp.getCode();
                if (0 != code) {
                    throw new BusinessException("req error:" + aiResp);
                }
                String reply = resolveReply(aiResp.getData());
                if (reply == null || reply.isBlank() || (!reply.endsWith("[end]") && !reply.endsWith("[error]"))) {
                    log.debug("id:{} on non complete data:{}", id, reply);
                    throw new RetryException();
                }
                return reply;
            })//
            .doOnError(err -> {
                if (!(err instanceof BusinessException) && !(err instanceof RetryException)) {
                    log.error("error", err);
                }
            })// 通信异常单独打印日志
            .retryWhen(Retry.fixedDelay(retry, Duration.ofSeconds(1))// 一秒一次 重试180次
                .filter(throwable -> !(throwable instanceof BusinessException))// 除了手动抛的业务异常，其他异常全部重试到超时
            );

    }

    public void askAI(AskAIReq askAIReq, Consumer<String> callback) {
        String finalHost = getGroupAiHost();
        log.info("askAI request url:{}, body:{}", finalHost + ASK_AI_URL, JsonUtil.toJsonStr(askAIReq));
        // todo Header
        postJsonAsync(finalHost + ASK_AI_URL, null, askAIReq, new TypeReference<AIResponse<String>>() {})//
            .flatMap(aiResult -> {
                if (aiResult.getCode() != 0) {
                    throw new BusinessException("as ai failed,req:" + askAIReq.getContent() + ",resp:" + aiResult);
                }
                String id = resolveRecordId(aiResult.getData());
                if (id == null || id.isBlank()) {
                    throw new BusinessException("as ai failed,no record id,req:" + askAIReq.getContent() + ",resp:" + aiResult);
                }
                return poll(finalHost, id);
            })//
            .onErrorResume(err -> {
                log.error("error", err);
                return Mono.just("[error]");
            })// 请求异常转成异常报文
            .flatMap(resp -> Mono.fromRunnable(() -> callback.accept(resp)).subscribeOn(scheduler))//
            .subscribe(voId -> log.info("ask done"), // Mono.empty 所以不生效，只需要处理异常
                exception -> log.error("error:", exception)// 异常都是callback的异常 只有打个日志算逑
            );
    }

    /**
     * 调用 AI Agent 但不回调回复（用于群聊普通消息推送场景）
     * 调用流程与 askAI 相同，但忽略 AI 的回复结果
     */
    public void askAINoReply(AskAIReq askAIReq) {
        String finalHost = getGroupAiHost();
        log.info("askAINoReply request url:{}, body:{}", finalHost + ASK_AI_URL, JsonUtil.toJsonStr(askAIReq));
        postJsonAsync(finalHost + ASK_AI_URL, null, askAIReq, new TypeReference<AIResponse<String>>() {})//
            .flatMap(aiResult -> {
                if (aiResult.getCode() != 0) {
                    throw new BusinessException("as ai failed,req:" + askAIReq.getContent() + ",resp:" + aiResult);
                }
                String id = resolveRecordId(aiResult.getData());
                if (id == null || id.isBlank()) {
                    throw new BusinessException("as ai failed,no record id,req:" + askAIReq.getContent() + ",resp:" + aiResult);
                }
                return poll(finalHost, id);
            })//
            .onErrorResume(err -> {
                log.error("error", err);
                return Mono.just("[error]");
            })//
            .subscribe(voId -> log.info("ask no reply done"), //
                exception -> log.error("error:", exception)//
            );
    }

    public List<AgentRecord> findAIRecord(AIRecordReq req) {
        var host = getGroupAiHost();
        AIResponse<List<AgentRecord>> listAIResponse = postJson(host + QUERY_RECORD_LIST_URL, null, req, new TypeReference<>() {
        });
        return listAIResponse.getData();
    }

    public List<RecordCountResp> countAIRecord(RecordCountReq req) {
        var host = getGroupAiHost();
        AIResponse<List<RecordCountResp>> listAIResponse = postJson(host + QUERY_RECORD_COUNT_URL, null, req, new TypeReference<>() {
        });
        return listAIResponse.getData();
    }

    /**
     * 查询智能体配置
     * @param agentId 智能体配置 ID
     * @return 智能体配置；查询失败或不存在返回 null
     */
    public AgentConfigVO queryAgentConfig(Long agentId) {
        if (agentId == null) {
            return null;
        }
        try {
            var host = getGroupAiHost();
            AIResponse<AgentConfigVO> resp = getJsonAsync(
                    host + QUERY_AGENT_CONFIG_URL + agentId, null,
                    new TypeReference<AIResponse<AgentConfigVO>>() {})
                .block();
            if (resp == null || resp.getCode() != 0 || resp.getData() == null) {
                log.warn("query agent config failed, agentId:{}, resp:{}", agentId, resp);
                return null;
            }
            return resp.getData();
        } catch (Exception e) {
            log.error("query agent config error, agentId:{}", agentId, e);
            return null;
        }
    }

    /**
     * 是否走透传模式：receiveIm=1 且 scope 为 0(所有) 或 2(仅IM)
     */
    public boolean isTransparentMode(AgentConfigVO config) {
        if (config == null) {
            return false;
        }
        if (!Integer.valueOf(1).equals(config.getReceiveIm())) {
            return false;
        }
        // scope: 0-所有, 1-仅AI问答, 2-仅IM; 仅 0 和 2 允许 IM 透传
        Integer scope = config.getScope();
        return scope != null && scope != 1;
    }

    /**
     * 是否完全不处理 IM 消息：scope=2(仅IM) 且 receiveIm!=1(不接收IM)
     * 该 agent 既不做 AI 问答，也不接收 IM，无任何处理必要
     */
    public boolean shouldSkipIm(AgentConfigVO config) {
        if (config == null) {
            return false;
        }
        return Integer.valueOf(2).equals(config.getScope())
                && !Integer.valueOf(1).equals(config.getReceiveIm());
    }

    public static class RetryException extends RuntimeException {}

    private String resolveRecordId(String data) {
        if (!isJsonObject(data)) {
            return data;
        }
        AskApprovalResp resp = JsonUtil.parseJson(data, AskApprovalResp.class);
        return resp == null || resp.getId() == null ? null : String.valueOf(resp.getId());
    }

    private String resolveReply(String data) {
        if (!isJsonObject(data)) {
            return data;
        }
        AskDetailResp resp = JsonUtil.parseJson(data, AskDetailResp.class);
        return resp == null ? null : resp.getReply();
    }

    private boolean isJsonObject(String data) {
        if (data == null || data.isBlank()) {
            return false;
        }
        try {
            return JsonUtil.objectMapper().readTree(data).isObject();
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    public static class AskApprovalResp {
        private Long id;
        private Boolean approvalRequired;
        private String approvalStatus;
        private Integer approveResult;
        private String approveNo;
        private String approveUrl;
        private String approveDetailUrl;
    }

    @Data
    public static class AskDetailResp {
        private Long id;
        private String reply;
        private Integer replyPosition;
        private Boolean replyPaused;
    }

}