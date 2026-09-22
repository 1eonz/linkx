package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "任务附件保存请求")
public class TasksAttachmentCreateReq {

    @Schema(description = "任务编号")
    @NotBlank(message = "taskNumber不能为空")
    private String taskNumber;

    @Schema(description = "任务状态")
    @NotBlank(message = "任务状态不能为空")
    private String status;

    @Schema(description = "文件列表")
    private List<TasksAttachmentVO> attachments;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 下一个处理人
     */
    @Schema(description = "下一个处理人信息")
    private List<TasksExecutors> nextExecutors;

}
