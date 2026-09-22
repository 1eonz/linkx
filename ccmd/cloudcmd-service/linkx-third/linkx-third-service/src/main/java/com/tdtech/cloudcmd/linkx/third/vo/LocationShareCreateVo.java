package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(description = "创建位置共享请求")
public class LocationShareCreateVo {

    @NotNull
    @Schema(description = "警信用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long userId;

    @NotBlank
    @Schema(description = "警信用户isdn", requiredMode = Schema.RequiredMode.REQUIRED, example = "100891")
    private String isdn;

    @NotBlank
    @Schema(description = "UDC群组号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "10089102")
    private String udcGroup;

    @Schema(description = "共享分发目标列表")
    private List<SharedTargetVo> sharedTargets;
}
