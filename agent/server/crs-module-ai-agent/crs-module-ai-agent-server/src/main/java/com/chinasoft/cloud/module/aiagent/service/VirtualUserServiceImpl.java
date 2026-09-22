package com.chinasoft.cloud.module.aiagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.common.pojo.CommonResult;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiAssistantAgentDto;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.UserVirtualVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.VirtualUserBindAgentVO;
import com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants;
import com.chinasoft.cloud.module.aiagent.msip.entity.VirtualUserLogin;
import com.chinasoft.cloud.module.aiagent.msip.entity.VirtualUserToken;
import com.chinasoft.cloud.module.aiagent.util.HttpsUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Validated
public class VirtualUserServiceImpl implements VirtualUserService {

    private static final String TOKEN_KEY = "linkx:token";

    private static final String LINKX_HOST = "LINKX_HOST";

    private static final String LINKX_CLIENT_ID = "LINKX_CLIENT_ID";

    private static final String LINKX_CLIENT_SECRET = "LINKX_CLIENT_SECRET";

    private static final String HTTPS_PREFIX = "https://";

    private static final String OPENAPI_PREFIX = "/linkx/openapi/v1";

    private static final String LOGIN_URL = OPENAPI_PREFIX + "/oauth/login";

    private static final String LOGOUT_URL = OPENAPI_PREFIX + "/oauth/logout";

    private static final String VIRTUAL_USER_LIST = OPENAPI_PREFIX + "/virtual-user/virtual";

    private static final String VIRTUAL_USER_BIND = OPENAPI_PREFIX + "/virtual-user/bind/list";

    private static final String AGENT_PAGE = OPENAPI_PREFIX + "/virtual-user/agent/page";

    private static final String AGENT_URL = OPENAPI_PREFIX + "/virtual-user/agent";

    private static final int RETRY_TIMES = 3;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GlobalsService globalsService;


    @Override
    public List<UserVirtualVO> listVirtualUsers(String userName) {
        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + VIRTUAL_USER_LIST;

        if (userName != null) {
            url += "?userName=" + userName;
        }

        try {
            return retryGet(url, new TypeReference<>() {
            }, RETRY_TIMES);
        } catch (Exception e) {
            log.error("获取虚拟用户列表失败", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_VIRTUAL_USER_LIST_ERROR);
        }
    }

    @Override
    public List<UserVirtualVO> selectBinsUser(List<Long> ids) {
        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + VIRTUAL_USER_BIND;

        String param = JsonUtils.toJsonString(ids);

        try {
            return retryPost(url, param, new TypeReference<>() {
            }, RETRY_TIMES);
        } catch (Exception e) {
            log.error("获取agent绑定的虚拟用户列表", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_AGENT_BIND_LIST_ERROR);
        }
    }

    @Override
    public Page<VirtualUserBindAgentVO> page(Long current, Long size, Long virtualUserId, Long agentId, Long createdUserId) {
        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + AGENT_PAGE;

        List<String> params = new ArrayList<>();

        if (current != null) {
            params.add("current=" + current);
        }

        if (size != null) {
            params.add("size=" + size);
        }

        if (virtualUserId != null) {
            params.add("virtualUserId=" + virtualUserId);
        }

        if (agentId != null) {
            params.add("agentId=" + agentId);
        }

        if (createdUserId != null) {
            params.add("createdUserId=" + createdUserId);
        }

        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }

        try {
            return retryGet(url, new TypeReference<>() {
            }, RETRY_TIMES);
        } catch (Exception e) {
            log.error("获取AI智能体绑定关系失败", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_AGENT_BIND_LIST_ERROR);
        }
    }

    @Override
    public Boolean update(AiAssistantAgentDto aiAssistantAgent) {
        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + AGENT_URL + "/" + aiAssistantAgent.getId();

        String param = JsonUtils.toJsonString(aiAssistantAgent);

        try {
            return retryPut(url, param, new TypeReference<>() {
            }, RETRY_TIMES);
        } catch (Exception e) {
            log.error("更新AI智能体绑定关系失败", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_UPDATE_AGENT_ERROR);
        }
    }

    @Override
    public AiAssistantAgentDto create(AiAssistantAgentDto aiAssistantAgent) {
        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + AGENT_URL;

        String param = JsonUtils.toJsonString(aiAssistantAgent);

        try {
            return retryPost(url, param, new TypeReference<>() {
            }, RETRY_TIMES);
        } catch (Exception e) {
            log.error("新增AI智能体绑定关系失败", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_CREATE_AGENT_ERROR);
        }
    }

    @Synchronized
    private void logout() {
        String token = stringRedisTemplate.opsForValue().get(TOKEN_KEY);

        if (token == null) {
            return;
        }

        String host = globalsService.findByName(LINKX_HOST);

        String url = HTTPS_PREFIX + host + LOGOUT_URL;

        String resp = HttpsUtil.post(url, "", token);

        var result = JsonUtils.parseObject(resp, new TypeReference<CommonResult<Void>>() {
        });

        if (result.getCode() != 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_USER_LOGOUT_ERROR);
        }

        stringRedisTemplate.delete(TOKEN_KEY);
    }

    @Synchronized
    private void reLogin() {
        stringRedisTemplate.delete(TOKEN_KEY);
        getToken();
    }

    @Synchronized
    private String getToken() {
        String token = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
        if (token != null) {
            return token;
        }

        String host = globalsService.findByName(LINKX_HOST);
        String clientId = globalsService.findByName(LINKX_CLIENT_ID);
        String clientSecret = globalsService.findByName(LINKX_CLIENT_SECRET);

        String url = HTTPS_PREFIX + host + LOGIN_URL;

        VirtualUserLogin userLogin = new VirtualUserLogin();
        userLogin.setClientId(clientId);
        userLogin.setClientSecret(clientSecret);

        String param = JsonUtils.toJsonString(userLogin);

        String resp = HttpsUtil.post(url, param);

        var result = JsonUtils.parseObject(resp, new TypeReference<CommonResult<VirtualUserToken>>() {
        });

        if (result.getCode() != 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_USER_LOGIN_ERROR);
        }

        token = result.getData().getAccessToken();

        if (token == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.LINKX_USER_LOGIN_ERROR);
        }

        stringRedisTemplate.opsForValue().set(TOKEN_KEY, token);

        return token;
    }

    private <T> T retryGet(String url, TypeReference<CommonResult<T>> typeReference, int retryCount) {
        if (retryCount <= 0) {
            throw new RuntimeException("获取数据失败");
        }
        try {
            String resp = HttpsUtil.get(url, getToken());
            var result = JsonUtils.parseObject(resp, typeReference);
            if (result.getCode() != 0) {
                log.error("获取数据失败, 重新登录尝试: {}", result.getMsg());
                if (result.getMsg().equals("token验证失败")) {
                    log.info("token验证失败, 重新登录尝试");
                    reLogin();
                    return retryGet(url, typeReference, retryCount - 1);
                }
                throw new RuntimeException("获取数据失败");
            }
            return result.getData();
        } catch (Exception e) {
            log.error("获取数据失败, 重新登录尝试", e);
            reLogin();
            return retryGet(url, typeReference, retryCount - 1);
        }
    }

    private <T> T retryPost(String url, String param, TypeReference<CommonResult<T>> typeReference, int retryCount) {
        if (retryCount <= 0) {
            throw new RuntimeException("获取数据失败");
        }
        try {
            String resp = HttpsUtil.post(url, param, getToken());
            var result = JsonUtils.parseObject(resp, typeReference);
            if (result.getCode() != 0) {
                log.error("获取数据失败: {}", result.getMsg());
                if (result.getMsg().equals("token验证失败")) {
                    log.info("token验证失败, 重新登录尝试");
                    reLogin();
                    return retryPost(url, param, typeReference, retryCount - 1);
                }
                throw new RuntimeException("获取数据失败");
            }
            return result.getData();
        } catch (Exception e) {
            log.error("获取数据失败, 重新登录尝试", e);
            reLogin();
            return retryPost(url, param, typeReference, retryCount - 1);
        }
    }

    private <T> T retryPut(String url, String param, TypeReference<CommonResult<T>> typeReference, int retryCount) {
        if (retryCount <= 0) {
            throw new RuntimeException("获取数据失败");
        }
        try {
            String resp = HttpsUtil.put(url, param, getToken());
            var result = JsonUtils.parseObject(resp, typeReference);
            if (result.getCode() != 0) {
                log.error("获取数据失败: {}", result.getMsg());
                if (result.getMsg().equals("token验证失败")) {
                    log.info("token验证失败, 重新登录尝试");
                    reLogin();
                    return retryPut(url, param, typeReference, retryCount - 1);
                }
                throw new RuntimeException("获取数据失败");
            }
            return result.getData();
        } catch (Exception e) {
            log.error("获取数据失败, 重新登录尝试", e);
            reLogin();
            return retryPut(url, param, typeReference, retryCount - 1);
        }
    }
}
