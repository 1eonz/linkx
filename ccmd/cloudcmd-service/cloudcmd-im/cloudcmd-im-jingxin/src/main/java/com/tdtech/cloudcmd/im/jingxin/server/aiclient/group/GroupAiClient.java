package com.tdtech.cloudcmd.im.jingxin.server.aiclient.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.aop.RetryUnauth;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.client.utils.RandomUtil;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.advice.SystemException;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class GroupAiClient {

    private static final Logger logger = LoggerFactory.getLogger(GroupAiClient.class);

    private static final String LOGIN_URL = "/openapi/v2/oauth/login";
    private static final String SEND_MESSAGES_URL = "/openapi/v2/messages";

    private static final String REDIS_LOCK_KEY = "cloudcmd:im:group:auth-lock:";
    private static final String REDIS_TOKEN_KEY = "cloudcmd:im:group:token:";
    // 发送消息场景独立 token 缓存：与 WS token 隔离，避免未绑定虚拟用户污染 userMap
    private static final String REDIS_SENDMSG_LOCK_KEY = "cloudcmd:im:group:sendmsg-auth-lock:";
    private static final String REDIS_SENDMSG_TOKEN_KEY = "cloudcmd:im:group:sendmsg-token:";

    // proxyUserId: clientId
    private final Map<Long, String> userMap = new ConcurrentHashMap<>();

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockFactory redisLockFactory;

    @Resource
    private HttpClient httpClient;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private AiVirtualUserInterface aiVirtualUser;

    public Long getProxyUserId(String userId) {
        ImToken token = getToken(userId);
        logger.debug("Group AI getProxyUserId token is: {}", token);
        if (Objects.nonNull(token)) {
            ImToken.ProxyUser proxyUser = token.getProxyUser();
            if (Objects.nonNull(proxyUser) && Objects.nonNull(proxyUser.getId())) {
                logger.info("Group AI proxy user: {}", proxyUser.getId());
                return proxyUser.getId();
            }
        }
        return null;
    }

    /**
     * 按 appId 获取虚拟用户的 proxyUserId（不要求已绑定智能体）。
     * 查询策略：优先走已绑定智能体用户的查询路径（selectByClientId，复用 WS token 缓存），
     * 命中则用现有 getProxyUserId，避免重复鉴权；未命中再走未绑定用户的独立鉴权路径
     * （selectAiVirtualUserIncludeUnboundByAppId + getSendMsgToken），独立 token key 不污染 userMap/WS 缓存。
     *
     * @param appId 虚拟用户 appId（X-App-Id 请求头传入）
     * @return proxyUserId；虚拟用户不存在或鉴权失败时返回 null
     */
    public Long getProxyUserIdByAppId(String appId) {
        // 优先走已绑定智能体用户的查询路径，命中则复用 WS token 缓存
        AiVirtualUser boundUser = aiVirtualUser.getUserByClientId(appId);
        if (boundUser != null) {
            return getProxyUserId(boundUser.getClientId());
        }

        // 未绑定智能体的虚拟用户走独立鉴权路径
        AiVirtualUser user = aiVirtualUser.getUserByAppIdIncludeUnbound(appId);
        if (user == null) {
            return null;
        }
        // 用独立 token key，避免 updateProxyUser 把未绑定用户写进 userMap 触发 WS 抖动
        ImToken token = getSendMsgToken(user.getClientId(), user.getClientSec());
        logger.debug("Group AI getProxyUserIdByAppId appId:{}, token is: {}", appId, token);
        if (Objects.nonNull(token)) {
            ImToken.ProxyUser proxyUser = token.getProxyUser();
            if (Objects.nonNull(proxyUser) && Objects.nonNull(proxyUser.getId())) {
                logger.info("Group AI proxy user by appId:{}, proxyUserId:{}", appId, proxyUser.getId());
                return proxyUser.getId();
            }
        }
        return null;
    }

    /**
     * 按 appId 校验虚拟用户是否存在（含未绑定智能体的）。
     * 仅做轻量 DB 查询，不走鉴权，供调用方在不触发 HTTP 调用的前提下做存在性校验。
     *
     * @param appId 虚拟用户 appId
     * @return true 存在；false 不存在
     */
    public boolean existsVirtualUserByAppId(String appId) {
        // getUserByAppIdIncludeUnbound 覆盖所有虚拟用户（含已绑定），一次查询即可
        return aiVirtualUser.getUserByAppIdIncludeUnbound(appId) != null;
    }

    /**
     * 发送消息场景专用的 token 获取：独立 redis key 前缀 + 不更新 userMap。
     * 复用 auth() 的鉴权逻辑（sec 非空时绕过 selectByClientId 的 agent 过滤）。
     * 失败兜底策略与 getToken 一致：塞 3 分钟空 token 防穿透。
     */
    private ImToken getSendMsgToken(String userId, String sec) {
        String tokenKey = imSendMsgTokenKey(userId);
        var imToken = redisUtil.get(tokenKey, ImToken.class);
        if (imToken != null) {
            return imToken;
        }
        var redisLock = redisLockFactory.newRedisLock(imSendMsgLockKey(userId), Duration.ofMinutes(3L));
        if (!redisLock.tryLock(30L, TimeUnit.SECONDS)) {
            logger.error("Group AI send-msg auth lock timeout, userId:{}", userId);
            return null;
        }
        try {
            // double check lock
            imToken = redisUtil.get(tokenKey, ImToken.class);
            if (imToken != null) {
                return imToken;
            }
            imToken = auth(userId, sec);
            if (imToken.getExpireIn() <= 0) {
                imToken.setExpireIn(60L * 60L * 24L);
            }
            redisUtil.set(tokenKey, imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));
            return imToken;
        } catch (Exception e) {
            // block on fail，塞 3 分钟空 token 防穿透
            imToken = new ImToken();
            imToken.setExpireIn(6L * 60L);
            redisUtil.set(tokenKey, imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));
            logger.error("Group AI send-msg auth error, userId:{}", userId, e);
            return null;
        } finally {
            redisLock.unlock();
        }
    }

    public Long getDefaultProxyUserId() {
        var user = aiVirtualUser.getDefaultUser();
        if (user == null) {
            throw new BusinessException("未找到默认群AI助手");
        }
        return getProxyUserId(user.getClientId());
    }

    public String getDefaultUserId() {
        var user = aiVirtualUser.getDefaultUser();
        if (user == null) {
            throw new BusinessException("未找到默认群AI助手");
        }
        return user.getClientId();
    }

    /**
     * 获取所有默认虚拟用户的 clientId 列表
     */
    public List<String> getDefaultUserIds() {
        List<AiVirtualUser> users = aiVirtualUser.getDefaultUsers();
        if (users.isEmpty()) {
            throw new BusinessException("未找到默认群AI助手");
        }
        return users.stream()
                .map(AiVirtualUser::getClientId)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有默认虚拟用户的 proxyUserId 列表，用于建群时拉多个群AI助手入群
     */
    public List<Long> getDefaultProxyUserIds() {
        List<AiVirtualUser> users = aiVirtualUser.getDefaultUsers();
        List<Long> proxyUserIds = new ArrayList<>(users.size());
        for (AiVirtualUser user : users) {
            Long proxyUserId = getProxyUserId(user.getClientId());
            if (Objects.nonNull(proxyUserId)) {
                proxyUserIds.add(proxyUserId);
            }
        }
        if (proxyUserIds.isEmpty()) {
            throw new BusinessException("未找到默认群AI助手");
        }
        return proxyUserIds;
    }

    public AiVirtualUser getUser(String userId) {
        return aiVirtualUser.getUserByClientId(userId);
    }

    public IMMsgRspVo sendMsg(ImMessageRequest<TxtMsgVo> imMessageRequest) {
        String userId = getDefaultUserId();
        return sendMsg(userId, imMessageRequest);
    }

    @RetryUnauth
    @SneakyThrows
    public IMMsgRspVo sendMsg(String userId, ImMessageRequest<TxtMsgVo> imMessageRequest) {
        logger.info("Group AI sendMsg imMessageRequest:{}", imMessageRequest);
        var host = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_HTTP_HOST_KEY);
        var gatewayPrefix = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_GATEWAY_KEY);
        var uri = new URI(host + gatewayPrefix + SEND_MESSAGES_URL);
        var headers = buildUserHeaders(userId);
        logger.info("Group AI uri:{}", uri);
        var json = httpClient.postJson(uri, headers, imMessageRequest, String.class);
        logger.info("Group AI sendMsg result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<IMMsgRspVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            TxtMsgVo msg = imMessageRequest.getMsg();
            String request = (msg != null) ? msg.getText() : "";
            // 上报告警
            reportAlarm(request);

            throw new SystemException("Group AI sendMsg error" + result);
        } else {
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_SEND_MSG_FAILED);
        }
        return result.getData();
    }

    public String getUserByProxyUser(String proxyUserId) {
        if (!StringUtils.isNumeric(proxyUserId)) {
            return null;
        }
        return userMap.getOrDefault(Long.parseLong(proxyUserId), null);
    }

    public String getUserByProxyUser(Long proxyUserId) {
        return userMap.getOrDefault(proxyUserId, null);
    }

    @Synchronized
    public void initGroupAi() {
        logger.info("init all group AI");
        List<AiVirtualUser> users = aiVirtualUser.getAllUser();
        Set<Long> allProxyUserIds = new HashSet<>();
        for (var user : users) {
            // 此处无需手动更新userMap，获取token时已经更新
            Long proxyUserId = getProxyUserId(user.getClientId());
            allProxyUserIds.add(proxyUserId);
        }

        for (var key : userMap.keySet()) {
            if (!allProxyUserIds.contains(key)) {
                userMap.remove(key);
            }
        }
    }

    public Map<Long, String> getUserMap() {
        if (userMap.isEmpty()) {
            initGroupAi();
        }
        return userMap;
    }

    public void addUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }

        if (userMap.containsValue(userId)) {
            // 已存在，不需要重新添加，减少查询
            return;
        }

        var user = aiVirtualUser.getUserByClientId(userId);
        if (user == null) {
            throw new BusinessException("User not found for ID: " + userId);
        }

        getToken(user.getClientId(), user.getClientSec());
    }

    public void addUser(AiVirtualUser user) {
        if (user == null) {
            return;
        }
        getToken(user.getClientId(), user.getClientSec());
    }

    public void addUser(List<AiVirtualUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        users.forEach(user -> getToken(user.getClientId(), user.getClientSec()));
    }

    public void removeUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        for (var map : userMap.entrySet()) {
            if (map.getValue().equals(userId)) {
                redisUtil.del(userTokenKey(map.getValue()));
                userMap.remove(map.getKey());
                break;
            }
        }
    }

    public void removeUser(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        for (var map : userMap.entrySet()) {
            if (userIds.contains(map.getValue())) {
                redisUtil.del(userTokenKey(map.getValue()));
                userMap.remove(map.getKey());
            }
        }
    }

    public void deprecateToken(String userId) {
        redisUtil.del(userTokenKey(userId));
        for (var map : userMap.entrySet()) {
            if (userId.equals(map.getValue())) {
                userMap.remove(map.getKey());
                break;
            }
        }
    }

    @Scheduled(fixedDelay = 50L * 1000L)
    public void updateGroupAi() {
        logger.info("rebuild user map info");
        initGroupAi();
        logger.info("build user map info success");
    }

    private Map<String, String> buildUserHeaders(String userId) {
        var token = getToken(userId);
        if (token == null) {
            throw new SystemException("Group AI auth failed");
        }
        if (token.getProxyUser() == null || token.getProxyUser().getId() == null) {
            throw new SystemException("Group AI no user binding found");
        }
        ImHeadersReq imHeadersReq = new ImHeadersReq();
        imHeadersReq.setUscc(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_USCC_KEY));
        imHeadersReq.setAllowSeid(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEID));
        imHeadersReq.setAllowSekey(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEKEY));
        imHeadersReq.setAuthorization(token.getAccessToken());
        imHeadersReq.setXClientId(userId);
        imHeadersReq.setXUserId(token.getProxyUser().getId());
        return imHeadersReq.toMap();
    }

    @SneakyThrows
    private ImToken auth(String userId, String sec) {
        var host = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_HTTP_HOST_KEY);
        var gatewayPrefix = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_GATEWAY_KEY);
        String clientSec;
        if (StringUtils.isBlank(sec)) {
            var user = aiVirtualUser.getUserByClientId(userId);
            if (user == null) {
                throw new BusinessException("Get group AI user info failed");
            }
            clientSec = user.getClientSec();
        } else {
            clientSec = sec;
        }
        var uri = new URI(host + gatewayPrefix + LOGIN_URL);
        var imAuthReq = new ImAuthReq();
        imAuthReq.setClientId(userId);
        imAuthReq.setClientSecret(clientSec);
        imAuthReq.setState(RandomUtil.randomString());

        ImHeadersReq imHeadersReq = new ImHeadersReq();
        imHeadersReq.setUscc(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_USCC_KEY));
        imHeadersReq.setAllowSeid(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEID));
        imHeadersReq.setAllowSekey(cachedImConfig.getConfig(GroupAiConstant.GLOBAL_ALLOW_SEKEY));
        logger.info("Group AI auth:{},{},{}", uri, imAuthReq, imHeadersReq);
        var respStr = httpClient.postJson(uri, imHeadersReq.toMap(), imAuthReq, String.class);
        logger.info("Group AI resp:{}", respStr);
        var resp = unWrapResponse(respStr, new TypeReference<ImResponse<ImToken>>() {
        });
        if (!Objects.equals(resp.getCode(), 0)) {
            // 203: 密码错误
            if (203 == resp.getCode()) {
                // 上报告警
                reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.SYSTEM_ACCOUNT_ERROR, userId);
            }
            throw new SystemException("Group AI auth failed " + resp);
        } else {
            // 擦除告警
            reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.SYSTEM_ACCOUNT_ERROR);
        }
        return resp.getData();
    }

    public ImToken getToken(String userId) {
        return getToken(userId, null);
    }

    /**
     * 不抛异常，异常的时候返回null
     *
     */
    public ImToken getToken(String userId, String sec) {
        String tokenKey = userTokenKey(userId);
        var imToken = redisUtil.get(tokenKey, ImToken.class);
        if (imToken != null) {
            updateProxyUser(userId, imToken);
            return imToken;
        }
        var redisLock = redisLockFactory.newRedisLock(userLockKey(userId), Duration.ofMinutes(3L));
        if (!redisLock.tryLock(30L, TimeUnit.SECONDS)) {
            logger.error("Group AI auth lock timeout");
            return null;
        }
        try {
            // double check lock
            imToken = redisUtil.get(tokenKey, ImToken.class);
            if (imToken != null) {
                updateProxyUser(userId, imToken);
                return imToken;
            }
            imToken = auth(userId, sec);
            if (imToken.getExpireIn() <= 0) {
                imToken.setExpireIn(60L * 60L * 24L);
            }
            redisUtil.set(tokenKey, imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));

            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED);

            updateProxyUser(userId, imToken);
            return imToken;
        } catch (Exception e) {
            // block on fail
            imToken = new ImToken();
            imToken.setExpireIn(6L * 60L);
            redisUtil.set(tokenKey, imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));
            logger.error("Group AI auth error", e);

            // 上报与警信服务器连接失败的告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED, e.getMessage());
            return null;
        } finally {
            redisLock.unlock();
        }
    }

    public <T> T unWrapResponse(String json, TypeReference<T> requireType) {
        var config = cachedImConfig.getConfig(GroupAiConstant.GLOBAL_UNWRAP_RESPONSE);
        if (Objects.equals(config, "true")) {
            var jsonObject = JsonUtil.parseJson(json);
            return jsonObject.getObject("data", requireType);
        } else {
            return JsonUtil.parseJson(json, requireType);
        }
    }

    private void reportAlarm(Object... param) {
        UserInfo user = SecurityUtils.getUser();
        String userName = Objects.nonNull(user) ? user.getUserName() : "";

        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_SEND_MSG_FAILED, userName, param);
    }

    private void clearAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.clearAlarm2MSIP(alarmTemplateZhEnum);
    }

    private String userLockKey(String userId) {
        return REDIS_LOCK_KEY + userId;
    }

    private String userTokenKey(String userId) {
        return REDIS_TOKEN_KEY + userId;
    }

    private String imSendMsgLockKey(String userId) {
        return REDIS_SENDMSG_LOCK_KEY + userId;
    }

    private String imSendMsgTokenKey(String userId) {
        return REDIS_SENDMSG_TOKEN_KEY + userId;
    }

    private void updateProxyUser(String userId, ImToken token) {
        if (Objects.nonNull(token)) {
            ImToken.ProxyUser proxyUser = token.getProxyUser();
            if (Objects.nonNull(proxyUser) && Objects.nonNull(proxyUser.getId())) {
                userMap.put(proxyUser.getId(), userId);
            }
        }
    }
}