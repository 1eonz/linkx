package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 三方任务附件信息表
 */
@TableName(value = "tb_tasks_attachment", autoResultMap = true)
@Data
@ToString
@Schema(description = "三方任务附件信息表")
public class TasksAttachment {

    @Schema(description = "主键ID")
    @TableId
    private Long id;

    @Schema(description = "任务编号")
    private String taskNumber;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件URL")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小(字节)")
    private Integer fileSize;

    @Schema(description = "创建人ID")
    private Long userId;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    @Schema(description = "删除标记：0-正常，1-删除")
    @TableLogic
    private Integer deleted;
}
