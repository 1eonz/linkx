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
public class TasksFavoriteDelReq implements Serializable {

    /**
     * 收藏动作的发起人身份证号
     */
    @Schema(description = "收藏动作的发起人身份证号")
    private String opUserId;

}
