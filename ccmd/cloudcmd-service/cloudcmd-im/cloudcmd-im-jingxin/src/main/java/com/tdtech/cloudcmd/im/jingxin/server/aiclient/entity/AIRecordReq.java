package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class AIRecordReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "身份证号")
    private List<String> identityCardNumber;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "时间范围")
    private String startTime;

    @Schema(description = "时间范围")
    private String endTime;
}