package com.chinasoft.cloud.module.aiagent.controller.app.qo;

import com.chinasoft.cloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAgentHistoryQO extends PageParam {

    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "智能体配置ID")
    private Long agentConfigId;

    @Schema(description = "上一页最后一条记录ID")
    private Long lastId;

    public Long getResolvedAgentId() {
        return agentConfigId != null ? agentConfigId : agentId;
    }

    @Override
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 100, message = "每页条数最大值为 100")
    public Integer getPageSize() {
        return super.getPageSize();
    }
}
