package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AgentConfigVO extends AgentConfig {

    @Schema(description = "智能体分类名称")
    private String categoryName;

}
