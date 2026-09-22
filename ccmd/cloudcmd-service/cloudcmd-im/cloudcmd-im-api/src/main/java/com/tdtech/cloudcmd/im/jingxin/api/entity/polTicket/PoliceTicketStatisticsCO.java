package com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class PoliceTicketStatisticsCO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "关联了协同群组的警单数量")
    private Long bindGroupTicketCount = 0L;

    @Schema(description = "关联了警单的协同群组数量")
    private Long bindTicketGroupCount = 0L;

    @Schema(description = "未关联警单的协同群组数")
    private Long unBindTicketGroupCount = 0L;

}

