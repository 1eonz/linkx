package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

/**
 * 群组与归档状态联表查询结果DTO
 * 包含CreateGroup和GroupExtends的关键字段
 */
@Data
public class GroupWithArchiveStatusDTO {
    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 群主ID
     */
    private String ownerId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 群组成员ID列表(逗号分隔)
     */
    private String userIds;

    /**
     * 归档状态：0-未归档，1-归档中，2-已归档
     */
    private Integer archived;

    /**
     * 群组类型
     */
    private Integer groupType;

    /**
     * 群组扩展ID
     */
    private Long groupExtendsId;

    /**
     * 群组成员ID列表(逗号分隔)
     */
    private String memberIds;
}
