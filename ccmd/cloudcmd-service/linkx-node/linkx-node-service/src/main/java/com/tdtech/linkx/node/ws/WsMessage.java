package com.tdtech.linkx.node.ws;

import com.tdtech.linkx.node.enums.WsMessageTypeEnum;
import com.tdtech.linkx.node.enums.WsMessageSubTypeEnum;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket消息体
 */
@Data
public class WsMessage<T> {

    /**
     * 消息唯一ID
     */
    private String msgId;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息子类型
     */
    private String subType;

    /**
     * 协议版本
     */
    private String version;

    /**
     * 消息数据
     */
    private T payload;

    /**
     * 消息说明
     */
    private String msg;

    /**
     * 时间戳
     */
    private Long timestamp;

    public WsMessage() {
        this.msgId = generateMsgId();
        this.timestamp = System.currentTimeMillis();
    }

    public WsMessage(String type) {
        this();
        this.type = type;
    }

    public WsMessage(String type, String subType) {
        this();
        this.type = type;
        this.subType = subType;
    }

    public WsMessage(String type, String subType, T payload) {
        this();
        this.type = type;
        this.subType = subType;
        this.payload = payload;
    }

    /**
     * 生成基于 UUID 的 msgId（去除中划线）。
     */
    public static String generateMsgId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建注册认证请求消息（服务端发起挑战）
     * 请求方生成 msgId，整个 register 流程复用同一个 msgId
     */
    public static WsMessage<Map<String, Object>> registerRequest(String challenge, long expiresIn) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.REGISTER.getType(),
                WsMessageSubTypeEnum.REQUEST.getType()
        );
        msg.setPayload(Map.of(
                "challenge", challenge,
                "timestamp", System.currentTimeMillis(),
                "expiresIn", expiresIn
        ));
        return msg;
    }

    /**
     * 创建注册认证响应消息（客户端返回JWT）
     * 复用入参 request 的 msgId
     */
    public static WsMessage<Map<String, Object>> registerResponse(String token, String msgId) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.REGISTER.getType(),
                WsMessageSubTypeEnum.RESPONSE.getType()
        );
        msg.setMsgId(msgId);
        msg.setPayload(Map.of("token", token));
        return msg;
    }

    /**
     * 创建注册认证成功消息
     * 复用入参 request 的 msgId
     */
    public static WsMessage<Map<String, Object>> registerSuccess(String sessionId, String peerId, String msgId) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.REGISTER.getType(),
                WsMessageSubTypeEnum.SUCCESS.getType()
        );
        msg.setMsgId(msgId);
        msg.setPayload(Map.of(
                "sessionId", sessionId,
                "timestamp", System.currentTimeMillis(),
                "peerInfo", Map.of("peerId", peerId, "status", "AUTHENTICATED")
        ));
        return msg;
    }

    /**
     * 创建注册认证失败消息
     * 复用入参 request 的 msgId
     */
    public static WsMessage<Map<String, Object>> registerFailure(String reason, String msgId) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.REGISTER.getType(),
                WsMessageSubTypeEnum.FAILURE.getType()
        );
        msg.setMsgId(msgId);
        msg.setPayload(Map.of(
                "reason", reason,
                "timestamp", System.currentTimeMillis()
        ));
        return msg;
    }

    /**
     * 创建心跳消息
     */
    public static WsMessage<Object> ping() {
        return new WsMessage<>(WsMessageTypeEnum.PING.getType());
    }

    /**
     * 创建心跳响应消息
     * 复用入参 ping 的 msgId
     */
    public static WsMessage<Object> pong(String msgId) {
        WsMessage<Object> msg = new WsMessage<>(WsMessageTypeEnum.PONG.getType());
        msg.setMsgId(msgId);
        return msg;
    }

    /**
     * 创建断开连接消息（单向通知，自动生成 msgId）
     */
    public static WsMessage<Object> disconnect(String reason) {
        WsMessage<Object> msg = new WsMessage<>(WsMessageTypeEnum.DISCONNECT.getType());
        msg.setMsg(reason);
        return msg;
    }

    /**
     * 创建开放数据授权通知消息（单向通知，自动生成 msgId）
     * payload 格式：
     * {
     *   "grantFromPeerId": "xxx",
     *   "grantToPeerId": "xxx",
     *   "permission": "org/dashboard/coopuser",
     *   "description": "xxx",
     *   "expiredTime": 1900000004000,
     *   "grantTime": 1700000004000
     * }
     */
    public static WsMessage<Map<String, Object>> auth(String grantFromPeerId, String grantToPeerId,
                                                       String permission, String description,
                                                       Long expiredTime, Long grantTime) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                WsMessageSubTypeEnum.AUTH.getType()
        );
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("grantFromPeerId", grantFromPeerId);
        payload.put("grantToPeerId", grantToPeerId);
        payload.put("permission", permission);
        if (description != null) {
            payload.put("description", description);
        }
        if (expiredTime != null) {
            payload.put("expiredTime", expiredTime);
        }
        payload.put("grantTime", grantTime != null ? grantTime : System.currentTimeMillis());
        msg.setPayload(payload);
        return msg;
    }

    /**
     * 创建开放数据取消授权通知消息（单向通知，自动生成 msgId）
     * payload 格式：
     * {
     *   "grantFromPeerId": "xxx",
     *   "grantToPeerId": "xxx",
     *   "permission": "org/dashboard/coopuser",
     *   "description": "xxx",
     *   "grantTime": 1700000004000
     * }
     */
    public static WsMessage<Map<String, Object>> unauth(String grantFromPeerId, String grantToPeerId,
                                                         String permission, String description, Long grantTime) {
        WsMessage<Map<String, Object>> msg = new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                WsMessageSubTypeEnum.UNAUTH.getType()
        );
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("grantFromPeerId", grantFromPeerId);
        payload.put("grantToPeerId", grantToPeerId);
        payload.put("permission", permission);
        if (description != null) {
            payload.put("description", description);
        }
        payload.put("grantTime", grantTime != null ? grantTime : System.currentTimeMillis());
        msg.setPayload(payload);
        return msg;
    }

    /**
     * 创建协同岗分享消息（单向通知，自动生成 msgId）
     */
    public static <D> WsMessage<D> share(D data) {
        return new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                WsMessageSubTypeEnum.SHARE.getType(),
                data
        );
    }

    /**
     * 创建取消协同岗分享消息（单向通知，自动生成 msgId）
     */
    public static <D> WsMessage<D> unshare(D data) {
        return new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                WsMessageSubTypeEnum.UNSHARE.getType(),
                data
        );
    }

    /**
     * 创建开放数据请求消息（请求方生成 msgId）
     */
    public static <D> WsMessage<D> openDataRequest(String subType, D data) {
        return new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                subType,
                data
        );
    }

    /**
     * 创建开放数据响应消息（复用入参 request 的 msgId）
     */
    public static <D> WsMessage<D> openDataResponse(String subType, D data, String msgId) {
        WsMessage<D> msg = new WsMessage<>(
                WsMessageTypeEnum.MESSAGE.getType(),
                subType,
                data
        );
        msg.setMsgId(msgId);
        return msg;
    }
}
