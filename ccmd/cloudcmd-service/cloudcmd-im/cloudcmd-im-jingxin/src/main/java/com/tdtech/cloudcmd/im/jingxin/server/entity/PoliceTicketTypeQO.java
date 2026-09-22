package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
@Schema(description = "警单类型查询条件")
public class PoliceTicketTypeQO {

    @Schema(description = "协同岗ID")
    private List<Long> postId;

}
