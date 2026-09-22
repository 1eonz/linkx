package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Schema(description = "协同岗位在线时长信息")
public class CollaborationPostOnlineDurationVO implements Serializable {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "协同岗名称", example = "运维协同岗")
    private String postName;

    @Schema(description = "所属组织ID", example = "1001")
    private Long orgId;

    @Schema(description = "所属组织编码", example = "ORG001")
    private String orgCode;

    @Schema(description = "所属组织名称", example = "运维部门")
    private String orgName;

    @Schema(description = "平均上线时间（秒）", example = "3600")
    private Long onlineDuration;
}
