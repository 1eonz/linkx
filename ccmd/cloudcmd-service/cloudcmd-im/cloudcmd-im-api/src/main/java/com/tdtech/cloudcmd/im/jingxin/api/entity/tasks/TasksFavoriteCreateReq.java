package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class TasksFavoriteCreateReq implements Serializable {

    /**
     * 收藏动作的发起人的身份证号
     */
    @Schema(description = "收藏动作的发起人的身份证号")
    private String opUserId;

    /**
     * 收藏动作的发起人姓名
     */
    @Schema(description = "收藏动作的发起人姓名")
    private String opUserName;

    @Schema(description = "收藏动作的发起人所属组织")
    private String opUserDepartment;

}
