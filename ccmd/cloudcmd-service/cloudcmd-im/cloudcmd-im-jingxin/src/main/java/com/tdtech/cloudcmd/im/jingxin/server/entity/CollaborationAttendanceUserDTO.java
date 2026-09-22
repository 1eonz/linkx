package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 协同岗上下岗接口传递用户信息实体
 * @author syf
 */
@Data
public class CollaborationAttendanceUserDTO {
    /**
     * im用户id
     */
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /**
     * im用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    private String userName;

    /**
     * 下岗方式
     */
    private Integer switchType;

    /**
     * 协同岗id
     */
    private Long postId;

    private String postName;
}
