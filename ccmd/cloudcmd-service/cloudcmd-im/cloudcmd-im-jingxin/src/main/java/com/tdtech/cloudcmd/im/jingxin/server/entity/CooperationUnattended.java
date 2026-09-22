package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@TableName(value = "tb_cooperation_unattended",autoResultMap = true)
@Data
@ToString
@Schema(description = "协同岗无人值守预警实体")
public class CooperationUnattended {

    @Schema(description = "主键ID")
    @TableId
    private Long id;

    @Schema(description = "协同岗ID")
    private Long postId;

    @Schema(description = "告警产生时间")
    private Date createTime;
}
