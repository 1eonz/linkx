package com.tdtech.cloudcmd.admin.resource.entity.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@Data
public class UserCommonAppSaveReqVO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String userId;

    @Schema(description = "终端类型", example = "1")
    private Integer terminalType;

    @Schema(description = "应用列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[{\"id\": 1, \"sort\": 1}, {\"id\": 2, \"sort\": 2}]")
    private List<AppVO> apps;

    @Schema(description = "用户常用应用创建/更新 Request VO")
    @Data
    @ToString(callSuper = true)
    public static class AppVO {

        @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
        private Long id;

        @Schema(description = "排序值", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "100")
        private Integer sort;
    }
}
