package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Data
@TableName("tb_group_care")
public class UserGroupCare {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * IM用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * IM群组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 创建时间（关注时间）
     */
    @TableField(value = "gmt_created")
    private Date gmtCreated;
}
