package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 应用类型定义表
 */
@Data
@TableName("tb_isdn_type_define")
@Schema(description = "应用类型定义信息")
public class IsdnTypeDefine {

    @TableId
    @Schema(description = "配置ID", example = "1")
    private Integer id;

    @Schema(description = "应用类型", example = "9")
    private String category;

    @Schema(description = "应用子类型", example = "1")
    private String subusercategory;

    @Schema(description = "应用子子类型", example = "109")
    private String apptype;

    @Schema(description = "类型名称", example = "终端用户")
    private String typeName;

    @Schema(description = "子类型名称", example = "记录仪")
    private String subTypeName;

    @Schema(description = "种类(内置/自定义)", example = "内置")
    private String kind;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "是否涉及(0-否,1-是)", example = "1")
    private Integer involve;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "修改时间")
    private Date gmtModified;
}
