package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(description = "更新群组成员请求体")
public class UpdateGroupMemberCO implements Serializable {

    @Schema(description = "警信用户ID，多个用户用\";\"分隔")
    private String userIds;

    @Schema(description = "用户身份证号，多个用户用\";\"分隔")
    private String idCards;

    @NotNull
    @Schema(description = "操作类型：1-添加成员；2-删除成员")
    private Integer opType;

    @NotNull
    @Schema(description = "入群方式。1：主动入群；3：邀请入群")
    private Integer joinType;

    @Schema(description = "加群附言")
    private String comment;
}
