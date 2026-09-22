package com.chinasoft.cloud.module.aiagent.framework.rpc.config;

import com.chinasoft.cloud.module.infra.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aiAgentRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {WebSocketSenderApi.class})
public class RpcConfiguration {}
