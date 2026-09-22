package com.chinasoft.cloud.module.aiagent.msip.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

public class ServletRequestContext {
    private static final String UNKNOWN = "unknown";

    private ServletRequestContext() {
        throw new UnsupportedOperationException();
    }

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String getIp() {
        return getIp(getRequest());
    }

    public static HttpServletRequest getRequest() {
        var requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return Objects.requireNonNull(((ServletRequestAttributes)requestAttributes).getRequest(),
                "method not working in servlet request Thread");
        } else {
            throw new RuntimeException("not servlet request,cant get servlet request");
        }
    }

    public static HttpServletResponse getResponse() {
        var requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return Objects.requireNonNull(((ServletRequestAttributes)requestAttributes).getResponse(),
                "method not working in servlet request Thread");
        } else {
            throw new RuntimeException("not servlet request,cant get servlet request");
        }
    }

    public static String getAppKey() {
        return getRequest().getHeader("X-CloudCmd-AppKey");
    }
}
