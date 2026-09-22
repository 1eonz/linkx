package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ChinasoftPortal
 * @date 2025/9/1
 * @Describe：
 */
@Getter
@Setter
@ToString
public class ImHeadersReq {

    private String uscc;

    private String allowSeid;

    private String allowSekey;

    private String authorization;

    private String xClientId;

    private Long xUserId;

    private Integer xUserType;

    // 将对象转换为 Map（HttpClient 通常需要 Map 形式的请求头）
    public Map<String, String> toMap() {
        Map<String, String> headerMap = new HashMap<>();
        // 非空时才添加，避免无效头
        if (authorization != null) headerMap.put("Authorization", authorization);
        if (xClientId != null) headerMap.put("X-Client-Id", xClientId);
        if (xUserId != null) headerMap.put("X-User-Id", xUserId.toString());
        if (xUserType != null) headerMap.put("X-User-Type", xUserType.toString());
        if (uscc != null) headerMap.put("uscc", uscc);
        if (allowSeid != null) headerMap.put("Allow-Seid", allowSeid);
        if (allowSekey != null) headerMap.put("Allow-Sekey", allowSekey);
        return headerMap;
    }
}
