package com.tdtech.cloudcmd.im.jingxin.server.entity;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PoliceTicketClientEnableCO {

    @NotNull
    private Long id;
    @NotNull
    private Integer status;

}
