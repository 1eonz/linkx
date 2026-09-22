package com.tdtech.linkx.node.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 透传响应 DTO，封装对端 proxy 响应的状态码、响应体、响应头。
 */
@Data
@AllArgsConstructor
public class ProxyResponse {

    private int statusCode;

    private byte[] body;

    private Map<String, List<String>> headers;
}
