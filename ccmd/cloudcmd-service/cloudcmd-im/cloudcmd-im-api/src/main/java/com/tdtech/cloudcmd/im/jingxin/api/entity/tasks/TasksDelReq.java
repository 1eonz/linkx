package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksDelReq implements Serializable {

    /**
     * 删除方式。1删除；2作废；
     */
    @NotNull(message = "type不能为空")
    @Schema(description = "删除方式。1删除；2作废；")
    private Integer type;

    /**
     * 删除原因
     */
    @NotBlank(message = "description不能为空")
    @Schema(description = "删除原因")
    private String description;

    /**
     * 删除动作的发起人姓名
     */
    @NotBlank(message = "opUserName不能为空")
    @Schema(description = "删除动作的发起人姓名")
    private String opUserName;

    /**
     * 删除动作的发起人所属组织
     */
    @NotBlank(message = "opUserDepartment不能为空")
    @Schema(description = "删除动作的发起人所属组织")
    private String opUserDepartment;

}
