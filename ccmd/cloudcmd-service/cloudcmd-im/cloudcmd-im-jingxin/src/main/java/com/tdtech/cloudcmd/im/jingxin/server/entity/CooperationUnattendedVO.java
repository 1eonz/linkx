package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class CooperationUnattendedVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "协同岗ID")
    private Long postId;

    @Schema(description = "协同岗名称")
    private String postName;

    @Schema(description = "所属组织名称")
    private String orgName;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "所属组织编码")
    private String orgCode;

    @Schema(description = "告警产生时间")
    private Date createTime;

    @Schema(description = "剩余在岗人数")
    private Integer lastPeopleNum = 0;
}
