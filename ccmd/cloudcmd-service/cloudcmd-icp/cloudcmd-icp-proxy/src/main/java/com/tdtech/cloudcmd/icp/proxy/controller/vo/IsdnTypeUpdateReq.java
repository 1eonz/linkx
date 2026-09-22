package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * ISDN类型更新请求
 */
@Data
@Schema(description = "ISDN类型更新请求")
public class IsdnTypeUpdateReq {

//    @Schema(description = "设备名称", example = "记录仪")
//    private String name;
//
//    @Schema(description = "设备类型", example = "9")
//    private String category;
//
//    @Schema(description = "设备子类型", example = "1")
//    private String subusercategory;
//
//    @Schema(description = "设备子子类型", example = "109")
//    private String apptype;
//
//    @Schema(description = "优先级", example = "5")
//    private String priority;

    @Schema(description = "设备类型图标的文件地址", example = "2026060414301234_recorder.png")
    @NotBlank(message = "设备类型图标的文件地址不能为空")
    private String icon;

    @Schema(description = "设备类型图标的uri地址", example = "/data/linkx/data/device-icon/2026060414301234_recorder.png")
    @NotBlank(message = "设备类型图标的uri地址不能为空")
    private String iconUri;
}
