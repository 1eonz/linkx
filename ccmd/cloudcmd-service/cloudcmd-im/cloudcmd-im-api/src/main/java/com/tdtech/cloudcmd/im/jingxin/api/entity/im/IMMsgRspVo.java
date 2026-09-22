package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "IM消息响应")
public class IMMsgRspVo implements Serializable {

    @Schema(description = "0-成功，非0-失败")
    private Integer code;

    @Schema(description = "响应消息")
    private String msg;

    @Schema(description = "转发结果列表")
    private List<Result> multiResult;

    @Data
    @Schema(description = "结果项")
    public static class Result implements Serializable {
        @Schema(description = "0-成功；非0-错误")
        private Integer code;

        @Schema(description = "错误描述")
        private String msg;

        @Schema(description = "服务器分配的msgId")
        private String msgId;

        @Schema(description = "接收人标识。用户ID/身份证号/群组号")
        private String toId;
    }
}