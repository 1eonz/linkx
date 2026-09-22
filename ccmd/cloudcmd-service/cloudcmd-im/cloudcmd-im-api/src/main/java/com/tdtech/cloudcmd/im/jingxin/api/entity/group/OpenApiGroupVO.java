package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "开放API群组列表项")
public class OpenApiGroupVO implements Serializable {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "群组名称")
    private String name;

    @Schema(description = "群组头像")
    private String avatar;

    @Schema(description = "群组类型")
    private String type;

    @Schema(description = "创建人ID")
    private Long ownerId;

    @Schema(description = "创建时间")
    private Date gmtCreated;
}
