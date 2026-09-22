package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务附件响应
 */
@Data
@Schema(description = "任务附件响应")
public class TasksAttachmentVO implements Serializable {

    @Schema(description = "附件ID")
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

}
