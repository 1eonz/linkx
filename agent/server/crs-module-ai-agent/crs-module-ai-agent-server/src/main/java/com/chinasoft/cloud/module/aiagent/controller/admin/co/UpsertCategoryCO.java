package com.chinasoft.cloud.module.aiagent.controller.admin.co;

import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "智能体分类")
@Data
public class UpsertCategoryCO {

    @Schema(description = "ID")
    @DiffLogField(name = "主键ID：不为空表示修改，为空表示新增")
    private Long id;

    @Schema(description = "名称")
    @NotBlank
    @DiffLogField(name = "名称")
    private String name;
}
