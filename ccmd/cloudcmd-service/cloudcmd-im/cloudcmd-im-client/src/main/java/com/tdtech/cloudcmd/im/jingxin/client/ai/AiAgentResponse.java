package com.tdtech.cloudcmd.im.jingxin.client.ai;

import lombok.Data;

/**
 * ai-agent 服务响应包装（对应 agent/server 侧 CommonResult 结构：code + data + msg）。
 * <p>
 * code=0 表示成功（与 ReactAiAgentClient 的 AIResponse 判断一致）。
 */
@Data
public class AiAgentResponse<T> {

    private Integer code;

    private T data;

    private String msg;
}