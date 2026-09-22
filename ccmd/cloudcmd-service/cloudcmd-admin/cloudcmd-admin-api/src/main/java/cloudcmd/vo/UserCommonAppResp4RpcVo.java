package cloudcmd.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserCommonAppResp4RpcVo implements Serializable {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String userId;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long appId;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer sort;

    @Schema(description = "应用信息，如果为null，则表示应用禁用或删除", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "示例应用")
    private AppInfoResp4RpcVO app;
}