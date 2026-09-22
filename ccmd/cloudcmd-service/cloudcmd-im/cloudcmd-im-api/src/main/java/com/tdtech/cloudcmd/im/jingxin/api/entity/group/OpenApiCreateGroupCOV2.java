package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class OpenApiCreateGroupCOV2 implements Serializable {

    @JsonIgnore
    @Schema(description = "警单ID")
    private Long ticketId;

    @NotNull
    @Schema(description = "警单单号")
    private String ticketNo;

    @NotNull
    @Schema(description = "身份证ID")
    private String idCard;

    @Schema(description = "群组名称")
    private String groupName;

}
