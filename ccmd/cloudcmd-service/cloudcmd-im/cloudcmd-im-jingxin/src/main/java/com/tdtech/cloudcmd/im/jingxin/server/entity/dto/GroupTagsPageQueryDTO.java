package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 群组标签分页查询DTO
 *
 * @author: S063874
 * @date: 2026-03-10
 */
@Data
@Schema(description = "群组标签分页查询参数")
public class GroupTagsPageQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "归档状态")
    private Integer archived;

    @Schema(description = "归档时间")
    private String archivedTime;

    @Schema(description = "关键字")
    private String keywords;

    @Schema(description = "群组名称")
    private String groupName;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "组织ID列表")
    private List<Long> orgIds;

    @Schema(description = "类型")
    private Integer type;
}