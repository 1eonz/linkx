package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Data
@ToString
@Validated
@NoArgsConstructor
public class AgentSubmissionUpdateCO {
    @NonNull
    @Schema(description = "申请状态")
    private AgentSubmission.AgentSubmissionStatusEnum status;
    @NonNull
    @Schema(description = "回复")
    private String reply;
    @NonNull
    @Schema(description = "操作人ID")
    private Long operId;
}
