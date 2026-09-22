package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 群组评价实体
 *
 * @author 群组归档评价
 * @date 2025/06/05
 */
@Data
@TableName("tb_group_rating")
public class GroupRating implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 评价人ID
     */
    @TableField("rater_id")
    private Long raterId;

    /**
     * 评价人类型（1-群主 2-成员）
     */
    @TableField("rater_type")
    private Integer raterType;

    /**
     * 被评价协同岗ID
     */
    @TableField("coop_user_id")
    private Long coopUserId;

    /**
     * 被评价协同岗名称
     */
    @TableField("coop_user_name")
    private String coopUserName;

    /**
     * 评价详情（JSON格式）
     */
    @TableField("rating_details")
    private String ratingDetails;

    /**
     * 评价评论
     */
    private String comment;

    /**
     * 平均评分
     */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /**
     * 评价时间
     */
    @TableField(value = "gmt_created", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreated;
}
