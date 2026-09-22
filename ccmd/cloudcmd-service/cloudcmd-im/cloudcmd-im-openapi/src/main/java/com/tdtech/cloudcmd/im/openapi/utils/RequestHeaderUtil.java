package com.tdtech.cloudcmd.im.openapi.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.util.json.JsonException;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class RequestHeaderUtil {

    public static <T> T getHeader(String name, Class<T> type) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String value = request.getHeader(name);
        if (StringUtils.isNotBlank(value)) {
            return type.cast(value);
        }
        return null;
    }

    public static <T> T getHeader(String name, TypeReference<T> typeReference) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String value = request.getHeader(name);
        if (StringUtils.isNotBlank(value)) {
            var obj = getJSONObject(value);
            return obj.toJavaObject(typeReference);
        }
        return null;
    }

    /**
     * 获取客户端用户ID, 仅app端会传，服务器端不会传
     *
     * @return 客户端用户ID
     */
    public static String getClientUserId() {
        return getHeader("X-User-Id", String.class);
    }

    public static String getClientIdCard() {
        return getHeader("X-Id-Card", String.class);
    }

    public static boolean validateXUserId(String xUserId, ImUserRpcApi imUserRpcApi) {
        if (StringUtils.isBlank(xUserId)) {
            return false;
        }
        try {
            Long.parseLong(xUserId);
        } catch (NumberFormatException e) {
            return false;
        }
        var result = imUserRpcApi.getUsersInfo(xUserId, null, xUserId);
        return result != null && result.getResults() != null && !result.getResults().isEmpty();
    }

    public static JsonObject getJSONObject(Object value) {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }

        if (value instanceof Map) {
            return new JsonObject((Map) value);
        }

        if (value instanceof String) {
            return JsonUtil.parseJson((String) value);
        }

        throw new JsonException(value + " is not a json object");
    }
}
