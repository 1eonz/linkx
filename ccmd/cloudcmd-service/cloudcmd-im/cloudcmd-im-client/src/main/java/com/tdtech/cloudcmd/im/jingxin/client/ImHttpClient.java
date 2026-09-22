package com.tdtech.cloudcmd.im.jingxin.client;

import cloudcmd.service.rpc.SystemConfigRpcService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.aop.RetryUnauth;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.client.utils.RandomUtil;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.advice.SystemException;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.cloudcmd.web.utils.methanol.MultipartBodyPublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ImHttpClient implements NamedLogger {

    private static final String LOGIN_URL = "/openapi/v2/oauth/login";
    // * 查询部门列表接口
    private static final String DEPARTMENTS_URL = "/openapi/v2/departments/page";
    // * 查询部门详情接口
    private static final String DEPARTMENT_URL = "/openapi/v2/departments";

    private static final String OFFICIAL_ACCOUNTS_PAGE_URL = "/openapi/v2/officialaccounts/page";
    private static final String ARTICLES_PAGE_URL = "/openapi/v2/articles/page";
    private static final String USER_URL = "/openapi/v2/users/page";
    private static final String USER_BY_DEPARTMENT_URL = "/openapi/v2/users";
    private static final String QUERY_GROUP_DETAIL = "/openapi/v2/group/";
    private static final String QUERY_USER_DETAIL = "/openapi/v2/users/userInfo";
    private static final String QUERY_USER_PROFILE = "/openapi/v2/users/profile";
    // 查询协同用户列表接口
    private static final String QUERY_USER_URL = "/openapi/addressbook/v2/cooperationusers/page";
    private static final String INSERT_USER_URL = "/openapi/addressbook/v2/cooperationusers";
    private static final String UPDATRE_USER_URL = "/openapi/addressbook/v2/cooperationusers/";
    private static final String DELETE_USER_URL = "/openapi/addressbook/v2/cooperationusers/";

    private static final String SUPPORT_USER_URL = "/openapi/addressbook/v2/cooperationusers/support";
    private static final int GROUP_SUPPORT_BATCH_SIZE = 100;

    private static final String UPLOAD_FILE_URL = "/openapi/v2/file/upload";
    private static final String DOWNLOAD_FILE_URL = "/openapi/v2/file/download/";
    private static final String CREATE_GROUP_URL = "/openapi/v2/group";
    private static final String ADD_GROUP_MEMBERS_URL = "/openapi/v2/group/%s/members";
    private static final String DELETE_GROUP_MEMBERS_URL = "/openapi/v2/group/%s/members";

    private static final String SEND_MESSAGES_URL = "/openapi/v2/messages";

    private static final String OFFLINE_MSG_URL = "/openapi/v2/messages/offline/page";

    // 订阅消息
    private static final String SUBSCRIBE_URL = "/openapi/v2/event/subscribe";

    // 指定用户的通讯录树形展示openApi
    private static final String USER_TREE_URL = "/openapi/v2/addressbook/users/%s/tree";

    // 分页获取指定用户的好友/关注列表openApi
    private static final String USER_FRIEND_URL = "/openapi/v2/friend/users/%s/page";

    private static final String VERSION_DATA = "/openapi/v2/base/version";

    private static final String MESSAGES_SENDBY_URL = "/openapi/v2/messages/sendby";
    private static final String MESSAGES_SENDBY_TARGE_TIME = "cloudcmd:im:message:target:time";
    /**
     * 下载超时时间
     */
    private static final String DOWNLOAD_FILE_TIMEOUT = "FILE_DOWNLOAD_FILE_TIMEOUT";

    /**
     * 下载重试延迟时间配置，逗号分隔，单位毫秒
     */
    private static final String DOWNLOAD_FILE_RETRY_DELAY = "FILE_DOWNLOAD_RETRY_DELAY";

    private Integer ATTEMPT_COUNT = 5;

    private final HttpClient httpClient;
    private final CachedImConfig cachedImConfig;
    private final RedisLockFactory redisLockFactory;
    private final RedisUtil redisUtil;
    private final ReportUtil reportUtil;
    @Getter
    private final ClientConfigGroup clientConfigGroup;
    private final ThreadPoolTaskExecutor groupSupportTaskExecutor;

    public String getName() {
        return clientConfigGroup.getName();
    }

    @SneakyThrows
    private ImToken auth() {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var cliId = cachedImConfig.getConfig(clientConfigGroup.getCliIdConfKey());
        var cliSec = cachedImConfig.getConfig(clientConfigGroup.getCliSecConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + LOGIN_URL);
        var imAuthReq = new ImAuthReq();
        imAuthReq.setClientId(cliId);
        imAuthReq.setClientSecret(cliSec);
        imAuthReq.setState(RandomUtil.randomString());
        String uscc = cachedImConfig.getConfig(clientConfigGroup.getHeadersUsccKey());
        String allowSeid = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSeidKey());
        String allowSekey = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSekeyKey());
        ImHeadersReq imHeadersReq = new ImHeadersReq();
        imHeadersReq.setUscc(uscc);
        imHeadersReq.setAllowSeid(allowSeid);
        imHeadersReq.setAllowSekey(allowSekey);
        log("info", "config:{} auth:{},{},{}", clientConfigGroup, uri, imAuthReq, imHeadersReq);
        var respStr = httpClient.postJson(uri, imHeadersReq.toMap(), imAuthReq, String.class);
        log("info", "resp:{}", respStr);
        var resp = unWrapResponse(respStr, new TypeReference<ImResponse<ImToken>>() {
        });
        if (!Objects.equals(resp.getCode(), 0)) {
            // {"code":203,"msg":"密码错误。"}
            if (203 == resp.getCode()) {
                // 上报告警
                reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.SYSTEM_ACCOUNT_ERROR, cliId);
            }
            throw new SystemException("auth failed " + resp);
        } else {
            // 擦除告警
            reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.SYSTEM_ACCOUNT_ERROR);
        }
        return resp.getData();
    }

    public void refreshToken() {
        deprecateToken();
        getToken();
    }

    //    public static void main(String[] args) {
    //        var json =
    //            "{\"code\":0,\"msg\":\"代理转发请求成功！！！\",\"data\":\"{\\\"code\\\":0,\\\"data\\\":{\\\"accessToken\\\":\\\"0E1AFC65B2E31E1E132E2054DDB2380A52BC6D1555528A5C7E485E152AD6C079CABD026F5F4AB5C210BF62FB2CFE4DAC40D6B7FF70B895C92A673FD031C25C10796E\\\",\\\"department\\\":{\\\"departmentCode\\\":\\\"130000718904306530\\\",\\\"departmentId\\\":1665329407,\\\"departmentName\\\":\\\"雄安新区公安局\\\"},\\\"expireIn\\\":-1,\\\"proxyUser\\\":{\\\"id\\\":27457759805983,\\\"isdn\\\":\\\"13402772\\\"},\\\"refreshToken\\\":\\\"E19098D96A92CAFAC356D7F18EB520FD86F1750223E81E8D049FCFB456488817D473F9B9EE2A88A83CF8CDD83602D78B39A9B7FF70B895C92A673FD031C25C10796E\\\",\\\"refreshTokenExpireIn\\\":-1,\\\"scope\\\":\\\"all\\\",\\\"tokenType\\\":\\\"client_credentials\\\"},\\\"msg\\\":\\\"操作成功\\\"}\"}";
    //        var jsonObject = JsonUtil.parseJson(json);
    //        var token = jsonObject.getObject("data", new TypeReference<ImResponse<ImToken>>() {
    //        });
    //        System.out.println(token);
    //    }

    public <T> T unWrapResponse(String json, Class<T> requireType) {
        var config = cachedImConfig.getConfig("IM_UNWRAP_RESPONSE");
        if (Objects.equals(config, "true")) {
            var jsonObject = JsonUtil.parseJson(json);
            return jsonObject.getObject("data", requireType);
        } else {
            return JsonUtil.parseJson(json, requireType);
        }
    }

    public <T> T unWrapResponse(String json, TypeReference<T> requireType) {
        var config = cachedImConfig.getConfig("IM_UNWRAP_RESPONSE");
        if (Objects.equals(config, "true")) {
            var jsonObject = JsonUtil.parseJson(json);
            return jsonObject.getObject("data", requireType);
        } else {
            return JsonUtil.parseJson(json, requireType);
        }
    }

    public JsonObject unWrapResponse(String json) {
        var config = cachedImConfig.getConfig("IM_UNWRAP_RESPONSE");
        if (Objects.equals(config, "true")) {
            var jsonObject = JsonUtil.parseJson(json);
            return jsonObject.getJSONObject("data");
        } else {
            return JsonUtil.parseJson(json);
        }
    }

    /**
     * 不抛异常，异常的时候返回null
     *
     */
    public ImToken getToken() {
        var imToken = redisUtil.get(clientConfigGroup.getImToken(), ImToken.class);
        if (imToken != null) {
            return imToken;
        }
        var redisLock = redisLockFactory.newRedisLock(clientConfigGroup.getImAuthRedisLock(), Duration.ofMinutes(3L));
        if (!redisLock.tryLock(30L, TimeUnit.SECONDS)) {
            log("error", "auth lock timeout");
            return null;
        }
        try {
            // double check lock
            imToken = redisUtil.get(clientConfigGroup.getImToken(), ImToken.class);
            if (imToken != null) {
                return imToken;
            }
            imToken = auth();
            if (imToken.getExpireIn() <= 0) {
                imToken.setExpireIn(60L * 60L * 24L);
            }
            redisUtil.set(clientConfigGroup.getImToken(), imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));

            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED);

            return imToken;
        } catch (Exception e) {
            // block on fail
            imToken = new ImToken();
            imToken.setExpireIn(6L * 60L);
            redisUtil.set(clientConfigGroup.getImToken(), imToken, Duration.ofSeconds(imToken.getExpireIn() / 2));
            log("error", "auth() error", e);

            // 上报与警信服务器连接失败的告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.IM_SERVER_IS_DISCONNECTED, e.getMessage());
            return null;
        } finally {
            redisLock.unlock();
        }
    }

    public void deprecateToken() {
        redisUtil.del(clientConfigGroup.getImToken());
    }

    public void clearSendMessageTargetTime() {
        try {
            redisUtil.del(MESSAGES_SENDBY_TARGE_TIME);
        } catch (Exception e) {
            log("error", "clearSendMessageTargetTime error");
        }
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<ImDepartment> departmentPage(@NotNull Integer pageNum, @NotNull Integer pageSize, String parentCode,
                                               String parentId, Integer includeChildren, Long tag) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + DEPARTMENTS_URL + "?pageNo=" + pageNum + "&pageSize=" + pageSize;
        includeChildren = includeChildren == null ? 0 : includeChildren;
        url += "&includeChildren=" + includeChildren;
        if (parentCode != null && !parentCode.isBlank()) {
            url += "&parentCode=" + parentCode;
        } else if (parentId != null && !parentId.isBlank()) {
            url += "&parentId=" + parentId;
        } else {
            // NOPE
        }
        if (tag != null) {
            url += "&tag=" + tag;
        }
        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "query departments result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<ImDepartment>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("query department error" + result);
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public List<ImDepartment> queryDepartment(String code) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + DEPARTMENT_URL + "?departmentCodes=" + code;

        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryDepartment url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "query department result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<DepartmentGetVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("query department error" + result);
        }
        return result.getData().getResults();
    }

    @RetryUnauth
    @SneakyThrows
    public List<ImDepartment> queryDepartmentByIds(String ids) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + DEPARTMENT_URL + "?departmentIds=" + ids;

        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryDepartmentById url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "queryDepartmentById result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<DepartmentGetVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("queryDepartmentById error" + result);
        }
        return result.getData().getResults();
    }

    @RetryUnauth
    @SneakyThrows
    public GroupVo queryGroupDetail(Long groupId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + QUERY_GROUP_DETAIL + groupId + "?isIncludeMember=1";

        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryGroupDetail url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "queryGroupDetail result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<GroupVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("query department error" + result);
        }
        return result.getData();
    }

    /**
     *
     * @param userId
     * @return
     */
    @RetryUnauth
    @SneakyThrows
    public ImPage<GroupInfoVo> queryGroupByUserId(@NotNull Integer pageNum, @NotNull Integer pageSize, Long userId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + QUERY_GROUP_DETAIL + "page" + "?userId=" + userId + "&pageNo=" + pageNum + "&pageSize=" + pageSize;
        ;

        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryGroupByUserId url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<GroupInfoVo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("query queryGroupByUserId error" + result);
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public List<UserIDNameInfo> queryUserDetail(String userId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + QUERY_USER_DETAIL + "?userIds=" + userId;

        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryUserDetail url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "queryUserDetail result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<List<UserIDNameInfo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("queryUserDetail error" + result);
        }
        return result.getData();
    }

    @SneakyThrows
    public ImUser userProfile(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + QUERY_USER_PROFILE + "?imToken=" + token;
        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "userProfile url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "userProfile result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImUser>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            log("error", "userProfile url:{}, resp: {}", url, result);
            // ImResponse(code=204, data=null, msg=用户不存在。)
            if (Objects.equals(result.getCode(), 204)) {
                // im的token失效了，调用的地方有null判断，这里return null，让客户端重新登录
                return result.getData();
            } else {
                // 其他异常还是保持抛出去
                throw new SystemException("queryUserDetail error" + result);
            }
        }
        return result.getData();
    }

    @org.jetbrains.annotations.NotNull
    private Map<String, String> buildHeaders() {
        var token = getToken();
        if (token == null) {
            throw new SystemException("auth failed");
        }
        if (token.getProxyUser() == null || token.getProxyUser().getId() == null) {
            throw new SystemException("no user binding found");
        }
        String uscc = cachedImConfig.getConfig(clientConfigGroup.getHeadersUsccKey());
        String allowSeid = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSeidKey());
        String allowSekey = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSekeyKey());
        String cliId = cachedImConfig.getConfig(clientConfigGroup.getCliIdConfKey());
        ImHeadersReq imHeadersReq = new ImHeadersReq();
        imHeadersReq.setUscc(uscc);
        imHeadersReq.setAllowSeid(allowSeid);
        imHeadersReq.setAllowSekey(allowSekey);
        imHeadersReq.setAuthorization(token.getAccessToken());
        imHeadersReq.setXClientId(cliId);
        imHeadersReq.setXUserId(token.getProxyUser().getId());
        return imHeadersReq.toMap();
    }

    @org.jetbrains.annotations.NotNull
    private Map<String, String> buildHeaders(Long userId) {
        var token = getToken();
        if (token == null) {
            throw new SystemException("auth failed");
        }
        if (token.getProxyUser() == null || token.getProxyUser().getId() == null) {
            throw new SystemException("no user binding found");
        }
        String uscc = cachedImConfig.getConfig(clientConfigGroup.getHeadersUsccKey());
        String allowSeid = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSeidKey());
        String allowSekey = cachedImConfig.getConfig(clientConfigGroup.getHeadersAllowSekeyKey());
        String cliId = cachedImConfig.getConfig(clientConfigGroup.getCliIdConfKey());
        ImHeadersReq imHeadersReq = new ImHeadersReq();
        imHeadersReq.setUscc(uscc);
        imHeadersReq.setAllowSeid(allowSeid);
        imHeadersReq.setAllowSekey(allowSekey);
        imHeadersReq.setAuthorization(token.getAccessToken());
        imHeadersReq.setXClientId(cliId);
        imHeadersReq.setXUserId(userId);
        imHeadersReq.setXUserType(0);
        return imHeadersReq.toMap();
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<ImUser> userPageByDepartment(@NotNull Integer pageNum, @NotNull Integer pageSize,
                                               String departmentCode, Integer includeChildren, String keywords, String deptId, String name, Long tag) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url =
                host + gatewayPrefix + USER_URL + "?pageNo=" + pageNum + "&pageSize=" + pageSize;

        ImToken token = getToken();
        String codeParam = StringUtils.isNotBlank(departmentCode) ? departmentCode
                : (Objects.nonNull(token) && Objects.nonNull(token.getDepartment()) && StringUtils.isNotBlank(
                token.getDepartment().getDepartmentCode()) ? token.getDepartment().getDepartmentCode()
                : "");


        if (StringUtils.isNotBlank(deptId)) {
            url += "&departmentId=" + deptId;
        } else {
            if (StringUtils.isNotBlank(codeParam)) {
                url += "&departmentCode=" + codeParam;
            }
        }
        if (includeChildren != null) {
            url += "&includeChildren=" + includeChildren;
        }
        if (keywords != null) {
            url += "&keywords=" + keywords;
        }
        if (StringUtils.isNotBlank(name)) {
            url += "&name=" + name;
        }
        if (tag != null) {
            url += "&tag=" + tag;
        }
        var uri = new URI(url);
        var headers = buildHeaders();
        var json = httpClient.getJson(uri, headers, String.class);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<ImUser>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            // 上报告警
            log("info", "query user url:{}  result:{}", url, result);
            reportAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_USER_INFO_FAILED, String.format("部门编码: %s", codeParam));
            throw new SystemException(result.getMsg());
        } else {
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_USER_INFO_FAILED);
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public UserGetVo userPage(String idCard, String userIds) {
        if ((idCard == null || idCard.isBlank()) && (userIds == null || userIds.isBlank())) {
            throw new BusinessException("参数错误");
        }
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var param = new LinkedHashMap<String, String>();
        param.put("idCards", idCard == null || idCard.isBlank() ? null : idCard);
        param.put("userIds", userIds == null || userIds.isBlank() ? null : userIds);
        var uri = HttpClient.buildUri(host + gatewayPrefix + USER_BY_DEPARTMENT_URL, param);
        var headers = buildHeaders();
        var json = httpClient.getJson(uri, headers, String.class);
        var result = unWrapResponse(json, new TypeReference<ImResponse<UserGetVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException(result.getMsg());
        }
        log("info", "query user by idCard:{} fail:{} result:{}", idCard, //
                result.getData() == null ? "[]" : result.getData().getFailures(),//
                safeSize(result.getData(), UserGetVo::getResults));
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public UserGetVo userPageById(String userIds) {
        if (StringUtils.isBlank(userIds)) {
            throw new BusinessException("参数错误");
        }
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var param = new LinkedHashMap<String, String>();
        param.put("userIds", userIds);
        var uri = HttpClient.buildUri(host + gatewayPrefix + USER_BY_DEPARTMENT_URL, param);
        var headers = buildHeaders();
        try {
            var json = httpClient.getJson(uri, headers, String.class);
            var result = unWrapResponse(json, new TypeReference<ImResponse<UserGetVo>>() {
            });
            if (!Objects.equals(result.getCode(), 0)) {
                throw new SystemException(result.getMsg());
            }
            log("info", "query user by userIds:{}, results: {}", userIds, result);
            return result.getData();
        } catch (Exception e) {
            log("warn", "userPageById {} error", userIds, e);
        }
        return null;
    }

    @RetryUnauth
    @SneakyThrows
    public UserGetVo userPageByIdOrIdCard(String userIds, String idCards) {
        if (StringUtils.isBlank(userIds) && StringUtils.isBlank(idCards)) {
            throw new BusinessException("参数错误");
        }

        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var param = new LinkedHashMap<String, String>();
        if (!StringUtils.isBlank(userIds)) {
            param.put("userIds", userIds);
        }

        if (!StringUtils.isBlank(idCards)) {
            param.put("idCards", idCards);
        }

        var uri = HttpClient.buildUri(host + gatewayPrefix + USER_BY_DEPARTMENT_URL, param);
        var headers = buildHeaders();
        try {
            var json = httpClient.getJson(uri, headers, String.class);
            var result = unWrapResponse(json, new TypeReference<ImResponse<UserGetVo>>() {
            });
            if (!Objects.equals(result.getCode(), 0)) {
                throw new SystemException(result.getMsg());
            }
            log("info", "query user by userIds:{}, results: {}", userIds, result);
            return result.getData();
        } catch (Exception e) {
            log("warn", "userPageByIdOrIdCard error, userIds:{}, idCards:{}", userIds, idCards, e);
        }
        return null;
    }


    @RetryUnauth
    @SneakyThrows
    public Long insertCollborationUser(UserCreateRequestBody userCreateRequestBody) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + INSERT_USER_URL);
        var headers = buildHeaders();
        log("info", "userCreateRequestBody:{}", userCreateRequestBody.getUserReq());
        var json = httpClient.postJson(uri, headers, userCreateRequestBody.getUserReq(), String.class);
        log("info", "insertCollborationUser result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<UniResponse>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            log("info", "insertCollborationUser error:{}" + result);
            throw new SystemException(result.getMsg());
        }
        return result.getData().getUserId();
    }

    @RetryUnauth
    @SneakyThrows
    public Integer subscribeMessage(List<SubscribeReq> requestList) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + SUBSCRIBE_URL);
        var headers = buildHeaders();
        log("info", "subscribeMessage requestList:{}", requestList);
        var json = httpClient.postJson(uri, headers, requestList, String.class);
        log("info", "subscribeMessage result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<UniResponse>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            log("info", "subscribeMessage error:{}" + result);
            throw new SystemException(result.getMsg());
        }
        return result.getCode();
    }

    @RetryUnauth
    @SneakyThrows
    public void updateCollborationUser(UserCreateRequestBody userCreateRequestBody, String id) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + UPDATRE_USER_URL + id);
        var headers = buildHeaders();
        log("info", "uri:{} userCreateRequestBody:{}", uri, userCreateRequestBody.getUserReq());
        var json = httpClient.putJson(uri, headers, userCreateRequestBody.getUserReq(), String.class);
        log("info", "updateCollborationUser resp:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<Void>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            log("info", "updateCollborationUser error:{}" + result);
            throw new SystemException(result.getMsg());
        }
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<UserListVo> queryCollborationUser(CooperationUserQueryRequest cooperationUserQueryRequest) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(
                host + gatewayPrefix + QUERY_USER_URL + "?pageNo=" + cooperationUserQueryRequest.getPageNo() + "&pageSize=" + cooperationUserQueryRequest.getPageSize() + "&includeChildren=1");
        var headers = buildHeaders();
        log("info", "queryCollborationUser:{} ", uri);
        var resultStr = httpClient.getJson(uri, headers, String.class);
        log("info", "queryCollborationUser:{} {}", uri, resultStr);
        var result = unWrapResponse(resultStr, new TypeReference<ImResponse<ImPage<UserListVo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            log("info", "updateCollborationUser error:{}" + result);
            throw new SystemException(result.getMsg());
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public void deleteCollborationUser(Long id) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + DELETE_USER_URL + id);
        var headers = buildHeaders();
        log("info", "id:{}", id);
        var json = httpClient.deleteJson(uri, headers, String.class);
        log("info", "deleteCollborationUser resp:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<Void>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException(result.getMsg());
        }
    }

    @RetryUnauth
    @SneakyThrows
    public String uploadFile(File file, String fileName, String fileType) {
        return uploadFile(file, fileName, 0);
    }

    /**
     * 上传文件，支持指定 category
     *
     * @param file     文件
     * @param fileName 文件名
     * @param category 文件分类。0-普通文件（默认）；其他取值按警信上传接口约定
     * @return 文件ID
     */
    @RetryUnauth
    @SneakyThrows
    public String uploadFile(File file, String fileName, int category) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + UPLOAD_FILE_URL);
        var headers = buildHeaders();
        var boundary = MultipartBodyPublisher.randomBoundary();
        var bodyBuilder = MultipartBodyPublisher.newBuilder().boundary(boundary);
        bodyBuilder.filePart("file", file.toPath());
        bodyBuilder.textPart("category", category);
        MultipartBodyPublisher body = bodyBuilder.build();
        String mediaType = body.mediaType().toString();
        log("info", "bodyBuilder:{}", file.getName());
        log("info", "boundary:{}", boundary);
        log("info", "headers:{}", headers);
        log("info", "mediaType:{}", mediaType);
        HttpResponse<byte[]> httpResponse =
                httpClient.sendRequest(uri, HttpClient.HttpMethodEnum.POST, headers, body, mediaType,
                        Duration.ofSeconds(30L));
        if (httpResponse.statusCode() != (HttpStatus.OK.value())) {
            throw new SystemException("upload file error" + httpResponse);
        }
        var respBody = new String(httpResponse.body(), StandardCharsets.UTF_8);
        log("info", "upload file result:{}", respBody);
        var result = unWrapResponse(respBody);
        var data = result.getJSONObject("data");
        if (data != null) {
            return data.getString("fileId");
        }
        return "";
    }

    @RetryUnauth
    @SneakyThrows
    public Long createGroup(CreateGroupReq createGroupReq) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + CREATE_GROUP_URL);
        var headers = buildHeaders();
        log("info", "uri:{} createGroupReq:{}", uri, createGroupReq);
        var json = httpClient.postJson(uri, headers, createGroupReq.getCreateGroupReq(), String.class);
        log("info", "createGroup resp:{}", json);
        var jsonObject = unWrapResponse(json);
        if (jsonObject.getInteger("code") != 0) {
            log("info", "createGroup error:{}" + json);
            // 上报告警
            reportAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_CREATE_A_GROUP_FAILED, createGroupReq.getCreateGroupReq().getName());

            throw new BusinessException(jsonObject.getString("msg"));
        } else {
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_CREATE_A_GROUP_FAILED);
        }
        return jsonObject.getJSONObject("data").getLong("groupId");
    }

    @RetryUnauth
    @SneakyThrows
    public JsonObject userTree(String userId, String deptId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = String.format(USER_TREE_URL, userId);
        if (StringUtils.isNotEmpty(deptId)) {
            url = url + "?departmentId=" + deptId;
        }
        var uri = new URI(host + gatewayPrefix + url);
        var headers = buildHeaders();
        log("info", "uri:{} getUserTreeReq:{}", uri, userId);
        var json = httpClient.getJson(uri, headers, String.class);
        var jsonObject = unWrapResponse(json);
        if (jsonObject.getInteger("code") != 0) {
            throw new BusinessException(jsonObject.getString("msg"));
        }
        return jsonObject.getJSONObject("data");
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<UserFollowVo> userFriend(String userId, int userType, int page, int pageSize) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = String.format(USER_FRIEND_URL, userId);
        var uri = new URI(host + gatewayPrefix + url + "?userType=" + userType + "&pageNo=" + page + "&pageSize=" + pageSize);
        var headers = buildHeaders();
        log("info", "uri:{} getUserFriendReq:{}", uri, userId);
        var json = httpClient.getJson(uri, headers, String.class);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<UserFollowVo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException(result.getMsg());
        }
        return result.getData();
    }

    private void reportAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum, Object... param) {
        UserInfo user = SecurityUtils.getUser();
        String userName = Objects.nonNull(user) ? user.getUserName() : "";

        reportUtil.saveAlarm2MSIP(alarmTemplateZhEnum, userName, param);
    }

    private void clearAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.clearAlarm2MSIP(alarmTemplateZhEnum);
    }

    @RetryUnauth
    @SneakyThrows
    public void groupSupport(Collection<CooperationUserGroupSupportUserReq> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }
        var promptSupportOffDutyIsShow = Integer.parseInt(cachedImConfig.getConfig("PROMPT_SUPPORT_OFF_DUTY_IS_SHOW"));
        var promptSupportOnDutyIsShow = Integer.parseInt(cachedImConfig.getConfig("PROMPT_SUPPORT_ON_DUTY_IS_SHOW"));
        var promptSupportPattern = cachedImConfig.getConfig("PROMPT_SUPPORT_PATTERN");
        var requestList = new ArrayList<>(reqs);
        for (var req : requestList) {
            if (req.getOpertype() == 0) {
                req.setIsShowPrompt(promptSupportOnDutyIsShow);
            } else {
                req.setIsShowPrompt(promptSupportOffDutyIsShow);
            }
            req.setPrompt(promptSupportPattern);
        }
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + SUPPORT_USER_URL);
        var headers = buildHeaders();
        var futures = new ArrayList<Future<?>>();
        for (int fromIndex = 0; fromIndex < requestList.size(); fromIndex += GROUP_SUPPORT_BATCH_SIZE) {
            var toIndex = Math.min(fromIndex + GROUP_SUPPORT_BATCH_SIZE, requestList.size());
            // 下游每次 groupSupport 请求最多接收 100 条支撑关系变更。
            var batch = new ArrayList<>(requestList.subList(fromIndex, toIndex));
            futures.add(groupSupportTaskExecutor.submit(() -> postGroupSupportBatch(uri, headers, batch)));
        }
        try {
            for (var future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException("groupSupport interrupted");
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new SystemException(cause == null ? e.getMessage() : cause.getMessage());
        }
    }

    private void postGroupSupportBatch(URI uri, Map<String, String> headers,
                                       List<CooperationUserGroupSupportUserReq> batch) {
        var resp = httpClient.postJson(uri, headers, batch, String.class);
        var result = unWrapResponse(resp, new TypeReference<ImResponse<Void>>() {
        });
        if (result.getCode() != 0) {
            throw new SystemException(result.getMsg());
        }
    }

    @RetryUnauth
    @SneakyThrows
    public byte[] downloadIcon(String fileId) {
        // 获取配置和 URI
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + DOWNLOAD_FILE_URL + fileId + "/0");

        // 构建请求头
        var headers = buildHeaders();
        return httpClient.get(uri, headers, Function.identity());
    }

    @RetryUnauth
    @SneakyThrows
    public Path downloadIconOrDefault(String fileId, Path filePath, Path defaultPath) {
        log("info", "download icon");
        // 获取配置和 URI
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + DOWNLOAD_FILE_URL + fileId + "/0");

        // 构建请求头
        var headers = buildHeaders();
        try {
            //            var response = httpClient.get(uri, headers, Function.identity());
            var response = getWithRetry(uri, headers);
            log("info", "download icon, uri：{},response size:{}", uri, response.length);
            // 创建父目录（如果不存在）
            Files.createDirectories(filePath.getParent());
            // 将字节数组写入文件
            Files.write(filePath, response);
            return filePath;
        } catch (HttpClient.HttpStatusException e) {
            log("warn", "download icon error,{} {}", e.getStatus(), e.getMessage());
            return defaultPath;
        }
    }

    private byte[] getWithRetry(URI uri, Map<String, String> headers) {
        for (int attempt = 1; attempt <= ATTEMPT_COUNT; attempt++) {
            log("info", "getWithRetry download times: {}, {}", attempt, uri);
            try {
                return httpClient.getWithTimeout(uri, headers, Function.identity(), getDownloadFileTimeout());
            } catch (HttpClient.HttpStatusException e) {
                log("warn", "getWithRetry download icon error,{} {}", e.getStatus(), e.getMessage());
                if (attempt == ATTEMPT_COUNT) {
                    log("warn", String.format("重试%d次仍失败", attempt));
                    throw e;
                }
            }
            // jitter 退避 0.5-1.5 s
//            long delay = 500 + ThreadLocalRandom.current().nextLong(0, 1001);
            // 阶梯抖动式延迟
            long delay = getDelay(attempt);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignore) {
            }
        }
        return null;
    }

    private long getDelay(int attempt) {
        String delayStr = cachedImConfig.getSysConfig(DOWNLOAD_FILE_RETRY_DELAY);
        if (StringUtils.isBlank(delayStr)) {
            delayStr = "1000,3000,10000,30000";
        }
        var delays = Arrays.stream(delayStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        Long baseDelay = delays.get(attempt-1);
        // ±10%抖动
        long jitter = ThreadLocalRandom.current().nextLong(0, (long)(baseDelay * 0.2) + 1);
        return baseDelay - (long)(baseDelay * 0.1) + jitter;
    }


    private Duration getDownloadFileTimeout() {
        String timeOutStr = cachedImConfig.getSysConfig(DOWNLOAD_FILE_TIMEOUT);
        if (StringUtils.isBlank(timeOutStr)) {
            timeOutStr = String.valueOf(5 * 60);
        }
        return Duration.ofSeconds(Long.parseLong(timeOutStr));
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<ImOfficialAccountWithStateVo> queryOfficialAccountsPage(Integer pageNum, Integer pageSize,
                                                                          Integer category, String name) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + OFFICIAL_ACCOUNTS_PAGE_URL + "?pageNo=" + pageNum + "&pageSize=" + pageSize;
        if (Objects.nonNull(category)) {
            url += "&category=" + category;
        }
        if (StringUtils.isNotBlank(name)) {
            url += "&name=" + name;
        }
        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryOfficialAccountsPage url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<ImOfficialAccountWithStateVo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("queryOfficialAccountsPage error" + result);
        }
        log("info", "queryOfficialAccountsPage result:{}", safeSize(result.getData(), ImPage::getRecords));
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public IMOfflineMsgPageVo offlineMsg(Long msgId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + OFFLINE_MSG_URL);
        var headers = buildHeaders();
        log("info", "offlineMsg url:{}", uri);
        var param =
                Map.of("msgIds", new Long[]{msgId}, "userId", getToken().getProxyUser().getId(), "limit", 1, "plaintext",
                        1);
        var json = httpClient.postJson(uri, headers, param, String.class);
        var resp = unWrapResponse(json, new TypeReference<ImResponse<IMOfflineMsgPageVo>>() {
        });
        if (!Objects.equals(resp.getCode(), 0)) {
            throw new SystemException("offlineMsg error" + resp);
        }
        log("info", "offlineMsg result:{}", safeSize(resp.getData(), a -> a.getImMsgs()));
        return resp.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public ImPage<ImArticleVo> queryArticlesPage(Integer pageNum, Integer pageSize, String title,
                                                 Long officialAccountId, Boolean isPublish, Long beginTime, Long endTime, Boolean isDel) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = host + gatewayPrefix + ARTICLES_PAGE_URL + "?pageNo=" + pageNum + "&pageSize=" + pageSize;
        if (StringUtils.isNotBlank(title)) {
            url += "&title=" + title;
        }
        if (Objects.nonNull(officialAccountId)) {
            url += "&officialaccountId=" + officialAccountId;
        }
        if (Objects.nonNull(isPublish)) {
            url += "&isPublish=" + isPublish;
        }
        if (Objects.nonNull(beginTime)) {
            url += "&beginTime=" + beginTime;
        }
        if (Objects.nonNull(endTime)) {
            url += "&endTime=" + endTime;
        }
        if (Objects.nonNull(isDel)) {
            url += "&isDel=" + isDel;
        }
        var uri = new URI(url);
        var headers = buildHeaders();
        log("info", "queryArticlesPage url:{}", url);
        var json = httpClient.getJson(uri, headers, String.class);
        log("info", "queryArticlesPage result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<ImPage<ImArticleVo>>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException("queryArticlesPage error" + result);
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public IMMsgRspVo sendMsg(ImMessageRequest imMessageRequest) {
        log("info", "sendMsg imMessageRequest:{}", imMessageRequest);
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + SEND_MESSAGES_URL);
        var headers = buildHeaders();
        log("info", "uri:{}", uri);
        var json = httpClient.postJson(uri, headers, imMessageRequest, String.class);
        log("info", "sendMsg result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<IMMsgRspVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            Object msg = imMessageRequest.getMsg();
            String request = msg instanceof TxtMsgVo ? ((TxtMsgVo) msg).getText() : "未知类型";
            // 上报告警
            reportAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_SEND_MSG_FAILED, request);

            throw new SystemException("sendMsg error: " + result.getMsg());
        } else {
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_SEND_MSG_FAILED);
        }
        return result.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public <T> IMMsgRspVo sendMsgByFrom(ImMessageRequest<T> imMessageRequest) {
        log("info", "sendMsg imMessageRequest:{}", imMessageRequest);
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + SEND_MESSAGES_URL);
        var headers = buildHeaders(imMessageRequest.getFrom());
        log("info", "uri:{}", uri);
        var json = httpClient.postJson(uri, headers, imMessageRequest, String.class);
        log("info", "sendMsg result:{}", json);
        var result = unWrapResponse(json, new TypeReference<ImResponse<IMMsgRspVo>>() {
        });
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException(result.getMsg());
        }
        return result.getData();
    }

    /**
     * 获取当前实例的代理用户id
     *
     * @return
     */
    public Long getProxyUserId() {
        ImToken token = getToken();
        log("info", "getProxyUserId token is: {}", token);
        if (Objects.nonNull(token)) {
            ImToken.ProxyUser proxyUser = token.getProxyUser();
            if (Objects.nonNull(proxyUser) && Objects.nonNull(proxyUser.getId())) {
                return proxyUser.getId();
            }
        }
        return null;
    }

    // 查询离线消息
    @RetryUnauth
    @SneakyThrows
    public IMOfflineMsgPageVo getIMOfflineMsg(MessagesOfflineReq messagesOfflineReq) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + OFFLINE_MSG_URL);
        var headers = buildHeaders();
        var token = getToken();
        messagesOfflineReq.setUserId(token.getProxyUser().getId());
        log("info", "query getIMOfflineMsg header ：{} messagesOfflineReq:{}", headers, messagesOfflineReq);
        var respStr = httpClient.postJson(uri, headers, messagesOfflineReq, String.class);
        var resp = unWrapResponse(respStr, new TypeReference<ImResponse<IMOfflineMsgPageVo>>() {
        });
        if (!Objects.equals(resp.getCode(), 0)) {
            // 上报告警
            reportAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED, "离线消息");

            throw new SystemException("getIMOfflineMsg error" + resp);
        } else {
            log("info", "getIMOfflineMsg result:{}", safeSize(resp.getData(), IMOfflineMsgPageVo::getImMsgs));
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED);
        }
        return resp.getData();
    }

    @RetryUnauth
    @SneakyThrows
    public void freeze(Long groupId) {
        log("info", "IMGroupInfoRes freeze:{}", groupId);
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        URI uri = new URIBuilder(host).setPath(gatewayPrefix + QUERY_GROUP_DETAIL + groupId + "/freeze").build();
        var headers = buildHeaders();
        log("info", "uri:{}", uri);
        var resultStr = httpClient.postJson(uri, headers, null, String.class);
        var result = unWrapResponse(resultStr, ImResponse.class);
        log("info", "freeze result:{}", result);
        if (!Objects.equals(result.getCode(), 0)) {
            throw new SystemException(result.getMsg());
        }
    }


    public IMSendbyPageMsgVo getSendMessage(IMSendbyMsgReq imSendbyMsgReq) throws URISyntaxException {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        var uri = new URI(host + gatewayPrefix + MESSAGES_SENDBY_URL);
        var headers = buildHeaders();

        log("info", "query getIMOfflineMsg header ：{} messagesOfflineReq:{}", headers, imSendbyMsgReq);
        var respStr = httpClient.postJson(uri, headers, imSendbyMsgReq, String.class);
        var resp = unWrapResponse(respStr, new TypeReference<ImResponse<IMSendbyPageMsgVo>>() {
        });
        if (!Objects.equals(resp.getCode(), 0)) {
            // 上报告警
            reportAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED, "离线消息");

            throw new SystemException("getIMOfflineMsg error" + resp);
        } else {
            // 擦除告警
            clearAlarm(AlarmTemplateZhEnum.INVOKING_THE_IM_TO_QUERY_CHAT_RECORDS_FAILED);
        }
        return resp.getData();
    }

    private <T> Integer safeSize(T data, Function<T, Collection<?>> func) {
        Collection<?> varr;
        return data == null ? 0 : (varr = func.apply(data)) == null ? 0 : varr.size();
    }

    @RetryUnauth
    @SneakyThrows
    public List<GroupInfoVo> queryGroupByUserIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        List<GroupInfoVo> allGroups = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 5000;
        int maxPages = 100; // 防止无限循环的最大页数限制

        while (pageNum <= maxPages) {
            ImPage<GroupInfoVo> pageResult = queryGroupByUserId(pageNum, pageSize, userId);
            if (pageResult == null || pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
                break;
            }
            allGroups.addAll(pageResult.getRecords());

            // 使用返回的总数来判断是否查询完所有数据
            long total = pageResult.getTotalCount();
            if (allGroups.size() >= total) {
                break;
            }
            pageNum++;
        }

        return allGroups;
    }

    @RetryUnauth
    @SneakyThrows
    public IMVersionMsgReq getVersion() {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey()).trim();
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey()).trim();
        try {
            String path = (gatewayPrefix + VERSION_DATA).replace(" ", "").trim();
            URI uri = new URIBuilder(host).setPath(path).build();

            var headers = buildHeaders();
            var respStr = httpClient.getJson(uri, headers, String.class);
            var resp = unWrapResponse(respStr, new TypeReference<ImResponse<IMVersionMsgReq>>() {
            });
            if (Objects.equals(resp.getCode(), 0)) {
                return resp.getData();
            }
            return null;
        } catch (Exception e) {
            log("info","获取版本接口异常", e);
        }
        return null;
    }

    @RetryUnauth
    @SneakyThrows
    public ImResponse<JsonObject> addGroupMembers(Long groupId, Object addMembersReq, Long operatorUserId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = String.format(ADD_GROUP_MEMBERS_URL, groupId);
        var uri = new URI(host + gatewayPrefix + url);
        var headers = buildHeaders(operatorUserId);
        log("info", "addGroupMembers uri:{}, req:{}", uri, addMembersReq);
        var json = httpClient.postJson(uri, headers, addMembersReq, String.class);
        log("info", "addGroupMembers resp:{}", json);
        var fullResponse = JsonUtil.parseJson(json);
        var resp = new ImResponse<JsonObject>();
        resp.setCode(fullResponse.getInteger("code"));
        resp.setMsg(fullResponse.getString("msg"));
        if (!fullResponse.containsKey("data") || fullResponse.get("data") == null) {
            return resp;
        }
        resp.setData(fullResponse.getJSONObject("data"));
        // 如果 data 中有业务状态码，使用业务状态码覆盖外层代理状态
        var data = resp.getData();
        if (data != null && data.containsKey("code")) {
            Integer dataCode = data.getInteger("code");
            if (dataCode != null) {
                resp.setCode(dataCode);
                if (data.containsKey("msg")) {
                    resp.setMsg(data.getString("msg"));
                }
                // 代理场景下 data 中可能还有内层 data（真正的业务数据），解包一层
                if (data.containsKey("data") && data.get("data") != null) {
                    resp.setData(data.getJSONObject("data"));
                }
            }
        }
        log("info", "addGroupMembers final resp code:{}, msg:{}, data:{}", resp.getCode(), resp.getMsg(), resp.getData());
        return resp;
    }

    @RetryUnauth
    @SneakyThrows
    public ImResponse<JsonObject> deleteGroupMembers(Long groupId, Object deleteMembersReq, Long operatorUserId) {
        var host = cachedImConfig.getConfig(clientConfigGroup.getHttpHostConfKey());
        var gatewayPrefix = cachedImConfig.getConfig(clientConfigGroup.getGatewayPrefixKey());
        String url = String.format(DELETE_GROUP_MEMBERS_URL, groupId);
        var uri = new URI(host + gatewayPrefix + url);
        var headers = buildHeaders(operatorUserId);
        log("info", "deleteGroupMembers uri:{}, req:{}", uri, deleteMembersReq);
        var resp = httpClient.sendJsonRequest(uri, HttpClient.HttpMethodEnum.DELETE, headers, deleteMembersReq,
                Duration.ofSeconds(30));
        var respBody = new String(resp.body(), StandardCharsets.UTF_8);
        log("info", "deleteGroupMembers resp:{}", respBody);
        var fullResponse = JsonUtil.parseJson(respBody);
        var imResp = new ImResponse<JsonObject>();
        imResp.setCode(fullResponse.getInteger("code"));
        imResp.setMsg(fullResponse.getString("msg"));
        if (!fullResponse.containsKey("data") || fullResponse.get("data") == null) {
            return imResp;
        }
        imResp.setData(fullResponse.getJSONObject("data"));
        // 如果 data 中有业务状态码，使用业务状态码覆盖外层代理状态
        var data = imResp.getData();
        if (data != null && data.containsKey("code")) {
            Integer dataCode = data.getInteger("code");
            if (dataCode != null) {
                imResp.setCode(dataCode);
                if (data.containsKey("msg")) {
                    imResp.setMsg(data.getString("msg"));
                }
                // 代理场景下 data 中可能还有内层 data（真正的业务数据），解包一层
                if (data.containsKey("data") && data.get("data") != null) {
                    imResp.setData(data.getJSONObject("data"));
                }
            }
        }
        return imResp;
    }
}
