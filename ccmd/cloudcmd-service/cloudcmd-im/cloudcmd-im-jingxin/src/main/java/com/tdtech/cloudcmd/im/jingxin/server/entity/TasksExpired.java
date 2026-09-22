package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@TableName(value = "tb_task_expired",autoResultMap = true)
@Data
@ToString
@Schema(description = "任务逾期告警实体")
public class TasksExpired {

    @Schema(description = "主键ID")
    @TableId
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "告警产生时间")
    private Date createTime;
}
