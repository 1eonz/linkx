package com.chinasoft.cloud.module.aiagent.controller.app.co;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "AI审批状态回调")
@Data
public class ApproveStatusCallbackCO {

    @Schema(description = "Linkx问答记录ID")
    @NotNull(message = "recordId cannot be null")
    private Long recordId;

    @Schema(description = "第三方审批单号")
    @NotBlank(message = "approveNo cannot be blank")
    private String approveNo;

    @Schema(description = "第三方审批详情H5地址")
    private String approveDetailUrl;

    @Schema(description = "给审批领导发送卡片的url")
    private String toLeaderUrl;

    @Schema(description = "审批状态：1-待审批，2-已完成")
    @NotBlank(message = "approvalStatus cannot be blank")
    @Pattern(regexp = "1|2", message = "approvalStatus must be 1 or 2")
    private String approvalStatus;

    @Schema(description = "审批结果：0-通过，1-驳回；approvalStatus=2时应传入")
    @Min(value = 0, message = "approveResult must be 0 or 1")
    @Max(value = 1, message = "approveResult must be 0 or 1")
    private Integer approveResult;

    @Schema(description = "审批意见或驳回原因")
    private String approveDescription;

    @Schema(description = "审批时间，建议格式yyyy-MM-dd HH:mm:ss")
    private String approveTime;

    @Schema(description = "审批人账号或姓名")
    private String approveUser;

    @AssertTrue(message = "approveResult must be empty when approvalStatus is 1, and cannot be null when approvalStatus is 2")
    public boolean isApproveStatusAndResultMatched() {
        // 状态只表达流程阶段，结果只表达通过/驳回，避免两类语义再次交织。
        if ("1".equals(approvalStatus)) {
            return approveResult == null;
        }
        if ("2".equals(approvalStatus)) {
            return approveResult != null;
        }
        return true;
    }
}
