package com.tdtech.cloudcmd.linkx.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "位置共享详情")
public class LocationShareDetailVo {

    @Schema(description = "位置共享Action ID")
    private Long id;

    @Schema(description = "创建人用户ID")
    private Long userId;

    @Schema(description = "UDC群组号码")
    private String udcGroup;

    @Schema(description = "状态。0：无效；1：有效")
    private int status;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreated;

    @Schema(description = "结束时间")
    private LocalDateTime closeTime;

    @Schema(description = "成员列表")
    private List<LocationShareMemberVo> members;
}
