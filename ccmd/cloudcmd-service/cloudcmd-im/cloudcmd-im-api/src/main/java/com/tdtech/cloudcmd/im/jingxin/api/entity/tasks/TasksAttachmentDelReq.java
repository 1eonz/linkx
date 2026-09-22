package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "任务附件删除请求")
public class TasksAttachmentDelReq {

    @Schema(description = "附件ID")
    private Long id;

    @Schema(description = "文件路径")
    @NotBlank(message = "文件路径不能为空")
    private String filePath;
}
