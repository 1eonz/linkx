package com.tdtech.cloudcmd.web.utils;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.constant.HeaderConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityUtils {

    private final static ThreadLocal<UserInfo> user = new ThreadLocal<>();

    public static void addUser(UserInfo userInfo) {
        user.set(userInfo);
    }

    public static UserInfo getUser() {
        return user.get();
    }

    public static void clear() {
        user.remove();
    }

    public static boolean isAdmin() {
        return Objects.requireNonNull(getUser()).isAdmin();
    }

    public static String getToken() {
        return getToken(ServletRequestContext.getRequest());
    }

    public static String getToken(HttpServletRequest request) {
        String authorization = request.getHeader(HeaderConstants.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            String[] auths = authorization.split(" ");
            if (auths.length != 2) {
                return null;
            }
            return auths[1];
        }
        return null;
    }

    public static class UnAuthException extends RuntimeException{
        public UnAuthException(String message) {
            super(message);
        }

        public UnAuthException() {
            super();
        }
    }
}
