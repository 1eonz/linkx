package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Data
@TableName("tb_group_extends")
public class GroupExtends {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    @TableField("archived")
    private Integer archived = 0; // 0:Not Archived, 1:Archiving, 2:Archived

    @TableField("archived_file")
    private String archivedFile;

    @TableField("archived_attachxxx_file")
    private String archivedAttachxxxFile;

    @TableField("archived_user_id")
    private Long archivedUserId;

    @TableField("archived_time")
    private LocalDateTime archivedTime;

    @TableField(value = "gmt_created", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreated;

    @TableField("group_type")
    private Integer groupType;
}