package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 排班类型。
 */
@Data
@TableName("tb_duty_type")
@Schema(description = "排班类型")
public class DutyType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排班类型标识，供排班记录和导入模板引用。
     */
    @TableId(value = "type", type = IdType.AUTO)
    @Schema(description = "排班类型标识，作为排班类型主键，供排班记录和导入模板引用，取值范围0-127", example = "1")
    private Long type;

    /**
     * 排班类型名称。
     */
    @Schema(description = "排班类型名称", example = "白班")
    private String name;

    /**
     * 创建人ID。
     */
    @Schema(description = "创建人ID", example = "10001")
    private Long createUserId;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2026-05-12 09:00:00")
    private Date gmtCreated;

    /**
     * Logical delete flag, 0 means active and 1 means deleted.
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    @Schema(description = "logical delete flag: 0 active, 1 deleted", example = "0")
    private Integer deleted = 0;

}
