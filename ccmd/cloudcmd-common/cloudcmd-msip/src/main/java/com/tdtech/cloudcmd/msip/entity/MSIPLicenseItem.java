package com.tdtech.cloudcmd.msip.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * License授权类
 */
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MSIPLicenseItem {

    // 资源项、开关项到期时间
    @Schema(description = "资源项、开关项到期时间")
    private String expireTime;

    // 资源项、开关项名称
    @Schema(description = "资源项、开关项名称")
    private String licenseControlItem;

    // 资源项、开关项类型：0 资源项，1 开关项，头元素信息（一般不返回）
    @Schema(description = "资源项、开关项类型：0 资源项，1 开关项，头元素信息（一般不返回）")
    private Integer licenseType;

    // 资源项、开关项的值
    @Schema(description = "资源项、开关项的值")
    private String resNum;
}
