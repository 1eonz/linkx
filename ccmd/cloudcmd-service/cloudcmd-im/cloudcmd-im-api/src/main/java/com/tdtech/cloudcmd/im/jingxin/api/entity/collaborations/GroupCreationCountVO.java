package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class GroupCreationCountVO implements Serializable {

    /**
     * 部门id
     */
    @Schema(description = "部门id")
    private String departmentId;
    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String departmentName;
    /**
     * 群组数量
     */
    @Schema(description = "群组数量")
    private Integer groupCount;
}
