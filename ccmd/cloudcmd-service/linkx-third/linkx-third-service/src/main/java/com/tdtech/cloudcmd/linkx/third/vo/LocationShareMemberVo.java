package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "位置共享成员信息")
public class LocationShareMemberVo {

    @Schema(description = "成员ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户ISDN")
    private String isdn;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;

    @Schema(description = "位置共享开始时间")
    private LocalDateTime gisShareStartTime;

    @Schema(description = "位置共享结束时间")
    private LocalDateTime gisShareEndTime;

    @Schema(description = "退出方式。0：手工退出；1：共享时间结束；2：用户状态异常")
    private int exitType;

    @Schema(description = "退出时间")
    private LocalDateTime exitTime;
}
