package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CooperationUserVO {
    @Schema(description = "绑定的协同岗id")
    private Long userId;

    @Schema(description = "绑定的协同岗姓名")
    private String name;

    @Schema(description = "绑定的协同岗isdn")
    private String isdn;

    @Schema(description = "支撑的群组id")
    private List<Long> groupIds;
}
