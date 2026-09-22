package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Getter;

/**
 * 文件类型枚举
 */
@Getter
public enum FileTypeEnum {

    /**
     * 图片
     */
    IMAGE(1, "image（图片）"),

    /**
     * 语音
     */
    AUDIO(2, "audio（语音）"),

    /**
     * 视频
     */
    VIDEO(3, "video（视频）"),

    /**
     * 其它类型文件
     */
    GENERAL(4, "general（其它类型文件）"),

    /**
     * Word文档
     */
    WORD(5, "word"),

    /**
     * Excel表格
     */
    EXCEL(6, "excel"),

    /**
     * PDF文档
     */
    PDF(7, "pdf"),

    /**
     * 文本文件
     */
    TXT(8, "txt"),

    /**
     * 短信文件
     */
    SMS(9, "sms"),

    /**
     * PPT演示文稿
     */
    PPT(10, "ppt"),

    /**
     * 位置文件
     */
    LOCATION(11, "location");

    /**
     * 文件类型编码
     */
    private final Integer code;

    /**
     * 文件类型描述
     */
    private final String description;

    FileTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 文件类型编码
     * @return 对应的枚举实例，若不存在则返回null
     */
    public static FileTypeEnum fromCode(Integer code) {
        for (FileTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
