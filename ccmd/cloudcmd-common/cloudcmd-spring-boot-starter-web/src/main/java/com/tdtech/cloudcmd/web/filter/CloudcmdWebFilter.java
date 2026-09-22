package com.tdtech.cloudcmd.web.filter;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartResolver;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.constant.HeaderConstants;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.web.config.WebConfigurationProperties;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-08-09 14:01
 */
@Slf4j
public class CloudcmdWebFilter implements Filter {

    private static final long timeFlag = 1000;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private MultipartResolver multipartResolver;
    @Resource
    private WebConfigurationProperties webConfigurationProperties;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
            var headerNames = httpServletRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                var s = headerNames.nextElement();
                log.debug("{}:{}", s, httpServletRequest.getHeader(s));
            }
        }
        ServletRequest requestWrapper = null;
        if (servletRequest instanceof HttpServletRequest) {
            // 这里如果是文件上传的流相关的数据，不能转换
            if (webConfigurationProperties.getWrapRequest() && !(this.multipartResolver != null
                && this.multipartResolver.isMultipart((HttpServletRequest)servletRequest))) {
                requestWrapper = new RequestWrapper((HttpServletRequest)servletRequest);
            }
        }
        if (requestWrapper == null) {
            doFilterHandle(servletRequest, servletResponse, filterChain);
        } else {
            doFilterHandle(requestWrapper, servletResponse, filterChain);
        }

    }

    private void fillAuth(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return;
        }
        String[] auths = authorization.split(" ");
        if (auths.length != 2) {
            log.debug("token length != 2");
            return;
        }
        var userInfo = redisUtil.get(AuthConstants.ACCESS_TOKEN_USER_KEY + auths[1], UserInfo.class);
        log.debug("current login user is {}", userInfo);
        if (userInfo != null) {
            SecurityUtils.addUser(userInfo);
        }else{
            log.info("未获取到当前登录人信息，authorization为：{}", authorization);
        }
    }

    private void doFilterHandle(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        long t1 = System.currentTimeMillis();
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        String authorization = httpServletRequest.getHeader(HeaderConstants.AUTHORIZATION);
        try {
            fillAuth(authorization);
        } catch (Exception e) {
            log.error("fill authorization error", e);
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            SecurityUtils.clear();
            var t = System.currentTimeMillis() - t1;
            if (t > timeFlag) {
                log.warn("this is a long time request,time:{} uri:{} ", t,
                    ((HttpServletRequest)servletRequest).getRequestURI());
            }
        }
    }
}
