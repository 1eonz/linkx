package com.tdtech.cloudcmd.im.jingxin.server.aiclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BCallReq;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BCallResp;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BGroupReq;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BLoginResp;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.OneOver1p4BResp;
import com.tdtech.cloudcmd.redis.RedisUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class React1Over1p4BClient extends BaseReactHttpClient {

    private static final String OneOver1p4B_TOKEN_REDIS_KEY = "cloudcmd:im:jingxin:OneOver1p4B:session";

    private static final String GROUP_REPORT_URL = "/third/init_upload/group_info";
    private static final String CALL_URL = "/third/call_14e";
    private static final String LOGIN_URL = "/user/login";
    private static final String LOG_URL = "proxy/ai/v1/deepseek-zjk/record";

    private static final String AI_AGENT_HTTP_HOST_CONF_KEY = "GROUP_AI_HOST";
    private static final String HOST_CONFIG_KEY = "XA_1o1p4B_HOST";
    private static final String USERNAME_CONFIG_KEY = "XA_1o1p4B_USERNAME";
    private static final String SCENE_CODE_CONFIG_KEY = "XA_1o1p4B_SCENE_CODE";
    private static final String PASSWORD_CONFIG_KEY = "XA_1o1p4B_PASSWORD";

    @Getter
    private final String[] configNames =
        new String[] {HOST_CONFIG_KEY, USERNAME_CONFIG_KEY, SCENE_CODE_CONFIG_KEY, PASSWORD_CONFIG_KEY};

    private final CachedImConfig cachedImConfig;
    private final Scheduler scheduler;
    private final RedisUtil redisUtil;

    public React1Over1p4BClient(CachedImConfig cachedImConfig,
        @Qualifier("groupAITaskExecutorService") ThreadPoolTaskExecutor executorService, RedisUtil redisUtil) {
        super();
        scheduler = Schedulers.fromExecutor(executorService);
        this.cachedImConfig = cachedImConfig;
        this.redisUtil = redisUtil;
    }

    public void cleanSession() {
        redisUtil.del(OneOver1p4B_TOKEN_REDIS_KEY);
    }

    private String getURI(String path) {
        return cachedImConfig.getConfig(HOST_CONFIG_KEY) + path;
    }

    private Mono<OneOver1p4BLoginResp> doLogin() {
        var username = cachedImConfig.getConfig(USERNAME_CONFIG_KEY);
        var password = cachedImConfig.getConfig(PASSWORD_CONFIG_KEY);
        var param = Map.of("username", username, "password", password);
        var uri = getURI(LOGIN_URL);
        return postJsonAsync(uri, null, param, new TypeReference<OneOver1p4BResp<OneOver1p4BLoginResp>>() {
        })//
            .map(resp -> {
                if (resp.getCode() != 0) {
                    throw new BusinessException("login failed:" + resp);
                }
                return resp.getData();
            });
    }

    private Mono<OneOver1p4BLoginResp> getToken() {
        return Mono.defer(() -> {
                var token = redisUtil.get(OneOver1p4B_TOKEN_REDIS_KEY, OneOver1p4BLoginResp.class);
                if (token == null) {
                    return doLogin()//
                        .doOnSuccess(newToken -> {
                            log.info("new token:{}", newToken);
                            redisUtil.set(OneOver1p4B_TOKEN_REDIS_KEY, newToken, Duration.ofHours(2L));
                        })//
                        .subscribeOn(scheduler);// Block part needs to run on defer executor
                } else {
                    return Mono.just(token);
                }
            })//
            .subscribeOn(scheduler);// Block part needs to run on defer executor

    }

    public void reportGroupAsync(String departmentCode, Integer groupType, Long groupId, String groupName,
        Runnable errorCallback) {
        var sceneCode = cachedImConfig.getConfig(SCENE_CODE_CONFIG_KEY);
        var uri = getURI(GROUP_REPORT_URL);
        getToken()//
            .flatMap(token -> {
                var param = new OneOver1p4BGroupReq(sceneCode, departmentCode, groupType, groupId,
                    groupName == null || groupName.isBlank() ? "default" : groupName);
                return postJsonAsync(uri, Map.of("token", token.getToken()), param,
                    new TypeReference<OneOver1p4BResp<OneOver1p4BGroupReq>>() {
                    });
            })//
            .flatMap(resp -> {
                if (resp.getCode() != 0) {// 业务错误 打印日志算了
                    log.error("report group failed, resp:{} departmentCode:{} groupType:{} groupId:{} groupName:{}",
                        resp, departmentCode, groupType, groupId, groupName);
                    errorCallback.run();
                }
                return Mono.empty();
            })//
            .doOnError(err -> {
                log.error("report group failed, departmentCode:{} groupType:{} groupId:{} groupName:{}", departmentCode,
                    groupType, groupId, groupName, err);
                errorCallback.run();
            }).subscribe();
    }

    public void doAskAsync(OneOver1p4BCallReq req, Consumer<OneOver1p4BCallResp> callback,
        Consumer<String> errorCallback) {
        var uri = getURI(CALL_URL);
        getToken()//
            .flatMap(token -> postJsonAsync(uri, Map.of("token", token.getToken()), req,
                new TypeReference<OneOver1p4BResp<OneOver1p4BResp<List<OneOver1p4BCallResp>>>>() {
                }))//
            .flatMap(resp -> {
                if (resp.getCode() != 0) {// 业务错误 打印日志算了
                    log.error("ask failed, resp:{} req:{} ", resp, req);
                    errorCallback.accept("系统错误");
                    return Mono.empty();
                }
                var resp2 = resp.getData();
                if (resp2.getCode() != 0) {// 业务错误 打印日志算了
                    log.error("ask failed, resp:{} req:{} ", resp, req);
                    errorCallback.accept("系统错误");
                    return Mono.empty();
                }
                var data = resp2.getData();
                if (data == null || data.isEmpty()) {
                    errorCallback.accept("未识别到人员");
                    return Mono.empty();
                }
                // Block part needs to run on defer executor
                return Mono.fromRunnable(() -> callback.accept(data.get(0))).subscribeOn(scheduler)
                    .thenReturn(data.get(0));
            })//
            .flatMap(data -> {
                // 拼接 idCard 和 name
                String content = String.format("姓名：%s，身份证号：%s", data.getName(), data.getIdCard());
                var loguri = cachedImConfig.getConfig(AI_AGENT_HTTP_HOST_CONF_KEY) + LOG_URL;
                var operatorInfo = req.getOperatorInfo();
                var body = Map.of("userName", operatorInfo.getOperatorName(),//
                    "identityCardNumber", operatorInfo.getOperatorIdCard(),//
                    "queryContent", content,//
                    "agentName", "人员核查",//
                    "agentConfigId", "-1",//
                    "departmentCode", operatorInfo.getDepartmentCode(),//
                    "departmentId", operatorInfo.getDepartmentId(),//
                    "departmentName", operatorInfo.getDepartmentName()//
                );
                return postAsync(loguri, null, body).onErrorContinue((e, o) -> log.error("log error", e))
                    .thenReturn(data);
            }).doOnError(err -> {
                log.error("ask failed, req:{}", req, err);
                errorCallback.accept("系统错误");
            }).subscribe();
    }

}
