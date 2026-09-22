package com.chinasoft.cloud.module.aiagent.controller.admin.qo;

import com.chinasoft.cloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "智能体查询记录")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecordQO extends PageParam {

    @Schema(description = "身份证号")
    private List<String> identityCardNumber;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "时间范围")
    private String startTime;

    @Schema(description = "时间范围")
    private String endTime;

}
