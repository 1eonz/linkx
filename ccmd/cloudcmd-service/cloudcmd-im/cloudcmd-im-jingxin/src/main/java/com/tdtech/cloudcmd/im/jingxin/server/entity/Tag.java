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
@TableName("tb_tags") // 关联数据库表名
public class Tag {
    @TableId(type = IdType.AUTO) // 主键自增策略
    private Long id;

    @TableField("name") // 对应表中字段名
    private String name; // 标签名称

    @TableField("icon") // 对应表中字段名
    private String icon; // 标签名称

    @TableField("color") // 对应表中字段名
    private String color; // 标签名称

    @TableField("create_user_id")
    private Long createUserId; // 创建人ID

    @TableLogic // 逻辑删除标记（0-未删除，1-已删除）
    @TableField("deleted")
    private Integer deleted = 0;

    @TableField(value = "gmt_created") // 插入时自动填充
    private Date gmtCreated;
}