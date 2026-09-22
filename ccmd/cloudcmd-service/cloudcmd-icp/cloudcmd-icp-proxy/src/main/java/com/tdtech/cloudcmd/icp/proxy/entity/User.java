package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.client.entity.UserResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_isdn")
@Schema(description = "用户信息")
public class User extends UserResp {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private Date createTime;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    /**
     * 逻辑删除字段：0-可用（未删除），1-禁用（已删除）
     */
    @TableLogic
    @Schema(description = "状态：0-表示可用，1-表示禁用（逻辑删除）", example = "0")
    private Integer status = 0;

    @Schema(description = "创建时间，由数据库自动管理")
    private Date gmtCreated;

    @Schema(description = "修改时间，由数据库自动管理")
    private Date gmtModified;

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
        this.createTime = gmtCreated;
    }
}
