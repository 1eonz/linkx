package com.tdtech.linkx.node.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * P2P dispatch 跨节点响应 DTO。
 * <p>
 * Dubbo RPC 返回值必须实现 Serializable，而 Spring 的 ResponseEntity 不可序列化，
 * 因此 RPC 接口用本 DTO 透传 HTTP 状态码与响应体，调用方按需解析。
 */
@Data
public class DispatchResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP 状态码
     */
    private int statusCode;

    /**
     * 响应体（可能为 null）
     */
    private byte[] body;

    public DispatchResponseDTO() {
    }

    public DispatchResponseDTO(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body;
    }
}