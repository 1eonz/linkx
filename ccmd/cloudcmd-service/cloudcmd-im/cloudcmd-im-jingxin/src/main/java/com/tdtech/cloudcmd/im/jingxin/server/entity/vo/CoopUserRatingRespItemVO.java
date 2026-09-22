package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 协同岗评价响应项对象
 */
@Data
public class CoopUserRatingRespItemVO {
    /**
     * 评价记录ID
     */
    private Long id;
    /**
     * 协同岗ID
     */
    private Long coopUserId;
    /**
     * 协同岗名称
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
