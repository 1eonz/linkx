package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/16
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "标签")
public class LabelVO  implements Serializable {
    @Schema(description = "ID")
    private Long id; // 主键ID

    @Schema(description = "标签名")
    private String name; // 标签名

    @Schema(description = "父级标签ID")
    private Long parentId; // 父级标签ID

    @Schema(description = "标签层级（1, 2, 3）")
    private Integer level; // 标签层级（1, 2, 3）

    @Schema(description = "子标签列表")
    private List<LabelVO> children;

    @Schema(description = "管理的协同岗id")
    private String collaborationIds;//管理的协同岗id

    @Schema(description = "协同岗名称")
    private String collaborationNames;

    // 图标
    @Schema(description = "图标")
    private String icon;

    // color
    @Schema(description = "颜色")
    private String color;

    @Schema(description = "类型，0 普通 1 1:14E")
    private Integer type;

    @Schema(description = "范围，0 所有 1 一键建群，2，职能建群")
    private Integer scope;

}
