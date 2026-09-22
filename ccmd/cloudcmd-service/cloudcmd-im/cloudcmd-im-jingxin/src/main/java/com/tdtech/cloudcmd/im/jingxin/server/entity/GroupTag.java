package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Data
@TableName("tb_group_tag")
public class GroupTag {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Collaboration group ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * Associated tag ID
     */
    @TableField("tag_id")
    private Long tagId;

    /**
     * Deletion status (0-Not deleted, 1-Deleted)
     */
    @TableField("is_deleted")
    private Integer isDeleted = 0;

    /**
     * Creator user ID
     */
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * Creation time
     */
    @TableField(value = "gmt_created")
    private Date gmtCreated;
}
