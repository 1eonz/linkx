package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * ISDN账号类型信息表
 */
@Data
@TableName("tb_isdn_type")
@Schema(description = "ISDN账号类型信息")
public class IsdnType {

    @TableId
    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "设备名称", example = "记录仪")
    private String name;

    @Schema(description = "设备类型", example = "9")
    private String category;

    @Schema(description = "设备子类型", example = "1")
    private String subusercategory;

    @Schema(description = "设备子子类型", example = "109")
    private String apptype;

    @Schema(description = "设备类型图标的文件地址", example = "/icon/recorder.png")
    private String icon;

    @Schema(description = "设备类型图标的uri地址", example = "/example/icon/recorder.png")
    private String iconUri;

    @Schema(description = "是否展示", example = "1")
    private Integer isShow = 1;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    @Schema(description = "最后修改时间")
    private Date gmtLastModified;
}
