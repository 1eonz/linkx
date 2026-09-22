package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 群组评价列表项对象
 */
@Data
public class GroupRatingListItemVO {
    /**
     * 评价记录ID
     */
    private Long id;
    /**
     * 群组ID
     */
    private Long groupId;
    /**
     * 评价人ID
     */
    private Long raterId;
    /**
     * 评价人类型（1-群主 2-成员）
     */
    private Integer raterType;
    /**
     * 被评价协同岗ID
     */
    private Long coopUserId;
    /**
     * 被评价协同岗名称
     */
    private String coopUserName;
    /**
     * 评价详情
     */
    private RatingDetailsVO ratingDetails;
    /**
     * 平均评分
     */
    private BigDecimal avgRating;
    /**
     * 评价时间
     */
    private LocalDateTime gmtCreated;
}
