package com.tdtech.cloudcmd.admin.resource.entity.vo;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class CarouselSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32745")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "图片URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @NotEmpty(message = "图片URL不能为空")
    private String pciUrl;

    @Schema(description = "链接地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    private String url;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    @Schema(description = "公众号ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long officialAccountId;

    @Schema(description = "公众号名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String officialAccountName;

    @Schema(description = "文章ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long articleId;
}
