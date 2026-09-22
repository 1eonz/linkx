package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class GroupQO implements Serializable {

    @NotNull
    private Long polTicketId;

}
