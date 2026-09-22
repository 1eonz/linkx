package com.tdtech.cloudcmd.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户和警信用户绑定的DTO
 *
 * @author 蔡永程
 * @version 1.0.0
 * @since 2026-05-19 14:19
 */
@Data
@Schema(description = "用户和警信用户绑定的DTO")
public class UserImUserRelDTO {

    @Schema(description = "用户ID 为空则表示为当前登录用户")
    private Long userId;

    @Schema(description = "警信用户ID")
    private Long imUserId;

}
