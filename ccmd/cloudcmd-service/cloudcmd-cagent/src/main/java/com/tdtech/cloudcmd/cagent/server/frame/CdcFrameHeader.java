package com.tdtech.cloudcmd.cagent.server.frame;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Frame Header:
 *
 * (int)frame length -> (short)type -> (int)sequence -> (byte)priority -> (byte)compress -> (short)token length ->
 * (short)sub system length -> (DYNAMIC)token -> (DYNAMIC)subsystem
 *
 */
@Getter
@Setter
@ToString
public class CdcFrameHeader {

    public static final String EMPTY_STRING = "";

    public static final byte TYPE_BIZ_REQ = 0x00;
    public static final byte TYPE_BIZ_RPN = 0x01;
    public static final byte TYPE_BIZ_NTY = 0x02;
    public static final byte TYPE_BIZ_LOGOUT = 0x03;
    public static final byte TYPE_BIZ_KICKOUT = 0x04; // 此类型消息收到后，cagent会关闭对应channel
    public static final byte TYPE_BIZ_TOKEN_REFRESH = 0x05; // token刷新
    public static final byte TYPE_BIZ_LOGIN = 0x06;
    public static final byte TYPE_BIZ_LOGOUT_NORMAL = 0x07;
    public static final byte TYPE_BIZ_LOGOUT_TIMEOUT = 0x08;

    /**
     * MESSAGE TYPE
     */
    public static final short TYPE_HANDSHAKE_WELCOME = 0x0010;
    public static final short TYPE_HANDSHAKE_HELLO = 0x0011;
    public static final short TYPE_HANDSHAKE_REGISTER = 0x0014;
    public static final short TYPE_HANDSHAKE_UNREGISTER = 0x0015;
    public static final short TYPE_HANDSHAKE_SUBSCRIBE = 0x0016;
    public static final short TYPE_HANDSHAKE_UNSUBSCRIBE = 0x0017;
    public static final short TYPE_HANDSHAKE_SUBSCRIBE_SUCCESS = 0x0018;
    public static final short TYPE_HANDSHAKE_SUBSCRIBE_FAIL = 0x0019;
    public static final short TYPE_HANDSHAKE_UNSUBSCRIBE_SUCCESS = 0x0022;
    public static final short TYPE_HANDSHAKE_UNSUBSCRIBE_FAIL = 0x0023;
    public static final short TYPE_HANDSHAKE_READY = 0x0012;
    public static final short TYPE_HANDSHAKE_REJECT = 0x0013;
    public static final short TYPE_PASSPORT_KICKOUT = 0x0028;
    public static final short TYPE_HANDSHAKE_HEARTBEAT = 0x0020;
    public static final short TYPE_HANDSHAKE_HEARTBEAT_RPN = 0x0021;
    public static final short TYPE_SUBSYSTEM_MESSAGE = 0X0002;
    public static final short TYPE_SEND_TO_SERVICE = 0x0030;
    public static final short TYPE_SWITCH_ENCODER = 0x0031;
    public static final short TYPE_UNKNOWN = 0x0000;

    /**
     * SEQUENCE 请求消息的序列号，客户端填写，服务端回填，用于匹配消息 从0开始填写，到最大MAX_SEQUENCE回滚
     */
    public static final int SEQ_DEFAULT = 0;
    /**
     * PRIORITY 消息优先级: 0~255。越大优先级越高，默认0
     */
    public static final byte PRIORITY_DEFAULT = 0x00;
    /**
     * COMPRESS 消息压缩 0x00：不压缩 0x01：zip
     */
    public static final byte COMPRESS_DEFAULT = 0x00;

    /**
     * 消息头的固定字段的总长度,不包括变长部分
     */
    public static final int FIX_HEADER_SIZE = 16;

    public static final int DEFAULT_SEND_STRATEGY = 0;
    public static final int QUEUE_CACHED_SEND_STRATEGY = 1;

    private short type = TYPE_UNKNOWN;
    private int sequence = SEQ_DEFAULT;
    private String subsystem = EMPTY_STRING;
    private byte priority = PRIORITY_DEFAULT;
    private byte compress = COMPRESS_DEFAULT;
    private String token = EMPTY_STRING;
    private int sendStrategy = DEFAULT_SEND_STRATEGY;

    public CdcFrameHeader() {

    }

    public CdcFrameHeader(short type) {
        this.type = type;
    }

    public CdcFrameHeader(short type, String subSystem) {
        this.type = type;
        this.subsystem = subSystem;
    }

    public CdcFrameHeader(short type, String token, String subSystem) {
        this.token = token;
        this.type = type;
        this.subsystem = subSystem;
    }

    public CdcFrameHeader(short type, String subSystem, byte priority, byte compress) {
        this.type = type;
        this.subsystem = subSystem;
        this.priority = priority;
        this.compress = compress;
    }

}