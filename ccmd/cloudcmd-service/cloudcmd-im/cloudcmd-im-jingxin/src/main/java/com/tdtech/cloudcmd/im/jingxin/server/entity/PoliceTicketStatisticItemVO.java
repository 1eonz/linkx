package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PoliceTicketStatisticItemVO {

    @Schema(description = "统计类型：0-关联了协同群组的警单数，1-关联了警单的协同群组数，2-未关联警单的协同群组数")
    private Integer type;

    @Schema(description = "具体数量")
    private Long count = 0L;

}
