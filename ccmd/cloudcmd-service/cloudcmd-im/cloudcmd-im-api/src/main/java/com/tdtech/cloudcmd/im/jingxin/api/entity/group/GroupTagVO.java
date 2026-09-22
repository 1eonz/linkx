package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@Schema(description = "群组标签视图对象")
public class GroupTagVO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name; // 标签名称

    @Schema(description = "标签图标")
    private String icon; // 标签图标

    @Schema(description = "标签颜色")
    private String color; // 标签颜色

    @Schema(description = "创建人ID")
    private Long createUserId; // 创建人ID

    @Schema(description = "是否删除(0-未删除 1-已删除)")
    private Integer deleted = 0;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    @Schema(description = "群组ID")
    private Long groupId;
}
