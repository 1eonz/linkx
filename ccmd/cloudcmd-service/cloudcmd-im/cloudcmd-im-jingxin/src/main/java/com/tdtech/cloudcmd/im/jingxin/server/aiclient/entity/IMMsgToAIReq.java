package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 5110 警信 IM 通道对话接入请求体
 * <p>
 * 接口路径：POST /ai/agent/v1/5110/chat/stream
 * 响应模式：SSE 流式
 * <p>
 * 三类处理分支：
 * <ul>
 *   <li>single_chat + at_xq(任意)：单聊回复，SSE 流式返回 LLM 回复</li>
 *   <li>group_chat + at_xq=true：群聊@回复，SSE 流式返回，含群历史上下文</li>
 *   <li>group_chat + at_xq=false：群聊纯推送，仅落库不调模型，SSE 快速结束</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IMMsgToAIReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 固定客户端类型，云侧用作通道埋点维度
     */
    public static final String CLIENT_TYPE_5110 = "5110";

    /**
     * 会话类型：单聊
     */
    public static final String CHAT_SOURCE_SINGLE = "single_chat";

    /**
     * 会话类型：群聊
     */
    public static final String CHAT_SOURCE_GROUP = "group_chat";

    /**
     * 消息类型：纯文本
     */
    public static final String MESSAGE_TYPE_TEXT = "text";

    /**
     * 消息类型：纯图片
     */
    public static final String MESSAGE_TYPE_IMAGE = "image";

    /**
     * 消息类型：文本+图片
     */
    public static final String MESSAGE_TYPE_TEXT_IMAGE = "text_image";

    /**
     * 用户原始文本消息内容。
     * 不用拼接用户ID，用户ID单独通过 user 字段发送。
     * 如果是纯图片类消息，该字段为空。
     */
    @Schema(description = "用户原始文本消息内容，纯图片时为空", example = "介绍一下人工智能")
    private String query;

    /**
     * 用户名(ID)，表明该条消息是哪个用户发送的
     */
    @Schema(description = "用户名(ID)", example = "王大鹏")
    private String user;

    /**
     * 用户身份证号
     */
    @Schema(description = "用户身份证号", example = "130xxx")
    private String id_card;

    /**
     * 该条消息的类型。
     * 取值：text | image | text_image
     */
    @Schema(description = "消息类型：text | image | text_image", example = "text")
    private String message_type;

    /**
     * 图片列表，纯文本消息时为空数组
     */
    @Schema(description = "图片列表，纯文本时为空数组")
    @Builder.Default
    private List<ImageItem> images = Collections.emptyList();

    /**
     * 固定值 "5110"，云侧用作通道埋点维度
     */
    @Schema(description = "固定值 5110", example = "5110")
    @Builder.Default
    private String client_type = CLIENT_TYPE_5110;

    /**
     * 会话类型：single_chat = 单聊 / group_chat = 群聊
     */
    @Schema(description = "会话类型：single_chat | group_chat", example = "single_chat")
    private String chat_source;

    /**
     * 当 chat_source=group_chat 时，标识群组名称
     */
    @Schema(description = "群聊时群组名称", example = "周例会群")
    private String group_name;

    /**
     * 当 chat_source=group_chat 时，标识群组描述
     */
    @Schema(description = "群聊时群组描述")
    private String group_description;

    /**
     * 5110 侧能唯一标识"一个对话窗口"的 ID。
     * 单聊场景为「用户 ↔ 小乔」窗口 ID；群聊场景为群窗口 ID。
     * 云侧通过 (session_id, chat_source) 联合键检索历史，单聊群聊 session_id 不可重复。
     */
    @Schema(description = "唯一对话窗口 ID，单聊群聊不可重复", example = "u10086")
    private String session_id;

    /**
     * 群聊场景下本条消息是否 @小乔需要回复。
     * 单聊场景云侧统一忽略此字段，一律给回复。
     */
    @Schema(description = "群聊是否@小乔需回复，单聊忽略", example = "false")
    private Boolean at_xq;

    /**
     * 图片项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 图片来源方式：url / base64
         */
        public static final String SOURCE_TYPE_URL = "url";
        public static final String SOURCE_TYPE_BASE64 = "base64";

        /**
         * 图片唯一标识，用于日志追踪、去重、多轮引用
         */
        @Schema(description = "图片唯一标识", example = "img_20250721_006")
        private String image_id;

        /**
         * 标识图片数据的来源方式：url / base64
         */
        @Schema(description = "图片来源方式：url | base64", example = "base64")
        private String source_type;

        /**
         * source_type=url 时为图片下载地址；
         * source_type=base64 时为图片 base64 编码内容
         */
        @Schema(description = "图片数据：url 或 base64")
        private String data;

        /**
         * 图片 MIME 类型，用于校验和解码。
         * 例：image/jpeg、image/png、image/webp
         */
        @Schema(description = "MIME 类型", example = "image/png")
        private String mime_type;

        /**
         * 图片大小（字节）
         */
        @Schema(description = "图片大小（字节）", example = "68")
        private Integer size;
    }
}