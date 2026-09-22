package com.chinasoft.cloud.framework.websocket.core.security;

import com.chinasoft.cloud.framework.common.enums.UserTypeEnum;
import com.chinasoft.cloud.framework.security.core.LoginUser;
import com.chinasoft.cloud.framework.security.core.filter.TokenAuthenticationFilter;
import com.chinasoft.cloud.framework.security.core.util.SecurityFrameworkUtils;
import com.chinasoft.cloud.framework.websocket.core.util.WebSocketFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * 登录用户的 {@link HandshakeInterceptor} 实现类
 *
 * 流程如下：
 * 1. 前端连接 websocket 时，会通过拼接 ?token={token} 到 ws:// 连接后，这样它可以被 {@link TokenAuthenticationFilter} 所认证通过
 * 2. {@link LoginUserHandshakeInterceptor} 负责把 {@link LoginUser} 添加到 {@link WebSocketSession} 中
 * 3. 如果没有 token，前端可以通过 ?userId={userId}&tenantId={tenantId} 参数建立连接（外部用户）
 *
 * @author 芋道源码
 */
@Slf4j
public class LoginUserHandshakeInterceptor implements HandshakeInterceptor {

    private static final String PARAM_ID_CARD = "idCard";
    private static final String PARAM_TENANT_ID = "tenantId";
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 1. 先尝试从 SecurityContext 获取 LoginUser（兼容现有 token 认证）
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        
        // 2. 如果没有 token 认证，从 URL 参数获取 userId（支持外部用户）
        if (loginUser == null) {
            String idCard = extractIdCard(request);
            if (StringUtils.hasText(idCard)) {
                loginUser = createVirtualLoginUser(idCard, request);
                log.info("[beforeHandshake][外部用户 WebSocket 连接，idCard({}) tenantId({})]",
                        loginUser.getIdCard(), loginUser.getTenantId());
            }
        }
        
        // 3. 存入 Session
        if (loginUser != null) {
            WebSocketFrameworkUtils.setLoginUser(loginUser, attributes);
            return true;
        }
        
        log.warn("[beforeHandshake][WebSocket 连接失败，无法识别用户]");
        return true; // 允许连接，但 Session 中没有用户信息
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // do nothing
    }

    /**
     * 从 URL 参数提取 idCard
     */
    private String extractIdCard(ServerHttpRequest request) {
        return extractParam(request, PARAM_ID_CARD);
    }

    /**
     * 从 URL 参数提取 tenantId（可选）
     */
    private Long extractTenantId(ServerHttpRequest request) {
        String tenantIdStr = extractParam(request, PARAM_TENANT_ID);
        if (!StringUtils.hasText(tenantIdStr)) {
            return DEFAULT_TENANT_ID;
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException e) {
            log.warn("[extractTenantId][tenantId 参数格式错误: {}，使用默认租户: {}]", tenantIdStr, DEFAULT_TENANT_ID);
            return DEFAULT_TENANT_ID;
        }
    }

    /**
     * 从 URL 提取参数值
     */
    private String extractParam(ServerHttpRequest request, String paramName) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * 创建虚拟 LoginUser（外部用户）
     */
    private LoginUser createVirtualLoginUser(String idCard, ServerHttpRequest request) {
        LoginUser loginUser = new LoginUser();
        loginUser.setIdCard(idCard);
        loginUser.setUserType(UserTypeEnum.MEMBER.getValue());
        loginUser.setTenantId(extractTenantId(request));
        return loginUser;
    }

}