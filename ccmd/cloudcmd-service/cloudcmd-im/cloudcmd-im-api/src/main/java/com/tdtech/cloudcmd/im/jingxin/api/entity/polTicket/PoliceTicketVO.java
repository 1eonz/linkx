package com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "警单")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PoliceTicketVO extends PoliceTicket  implements Serializable {

    @Schema(description = "是否绑定当前群组，分页按群组查询才有,0 未绑定 1 绑定")
    private Integer bindFlag;
}
