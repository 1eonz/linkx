package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(description = "发送IM消息请求体")
public class SendImMessageCO implements Serializable {

    @NotNull
    @Schema(description = "消息类型。1：文本消息；2：彩信消息；5：卡片消息")
    private Integer msgType;

    @Schema(description = "接收人的用户ID列表，多用户用\";\"分隔")
    private String targetUserIds;

    @Schema(description = "接收人的用户身份证号，多用户用\";\"分隔")
    private String targetIdCards;

    @Schema(description = "接收群组的群组号码，多用户用“;”分隔")
    private String groupIds;

    @Schema(description = "卡片内容。msgType=5时必填")
    private CardMessage card;

    @Schema(description = "文本内容。最大长度10000，支持内置表情和@人。msgType=1时必填")
    private String content;

    @Schema(description = "彩信文件ID。msgType=2时必填")
    private String fileId;

    @Schema(description = "指定发送消息的虚拟用户appId。不传用默认虚拟用户发送，传则用该虚拟用户发送",
            example = "app_123456")
    private String appId;
}