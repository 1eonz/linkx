package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 彩信消息体
 * 对应警信 IM 上传/发送接口的 MMSMsgVo
 */
@Getter
@Setter
@ToString
public class MMSMsgVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件类型。1-image；2-audio；3-video；4-general；5-word；6-excel；7-pdf；8-txt；9-sms；10-ppt；11-location
     */
    private Integer fileType;
    /**
     * 文件大小，单位KB。最大100M
     */
    private Long fileSize;
    /**
     * 文件名称，必选。携带文件后缀。最大长度:255
     */
    private String fileName;
    /**
     * 必选。文件管理服务分配的filekey
     */
    private String fileKey;
    /**
     * 图片尺寸，格式为w_h，可选。图片时携带
     */
    private String imageSize;
    /**
     * 视频封面图片，可选。视频时携带
     */
    private String videoThumb;
    /**
     * 时长（秒），可选。语音/视频时携带
     */
    private Long duration;
    /**
     * 是否阅后即焚。携带即表示该文件需阅后即焚
     */
    private Boolean selfDestruct;
}