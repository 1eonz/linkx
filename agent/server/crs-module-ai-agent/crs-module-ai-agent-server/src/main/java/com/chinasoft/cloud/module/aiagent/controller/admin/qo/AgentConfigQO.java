package com.chinasoft.cloud.module.aiagent.controller.admin.qo;

import com.chinasoft.cloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "智能体配置")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentConfigQO extends PageParam {
    @Schema(description = "名称（模糊查询）")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(hidden = true)
    private Integer type;

    @Schema(description = "作用域列表（0：所有；1：仅AI智能体问答；2：仅IM）。不传则不过滤")
    private List<Integer> scopeList;
}