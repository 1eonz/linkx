package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingWithGroupInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价ID
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
     * 评价人类型
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
     * 评价详情（JSON格式）
     */
    private String ratingDetails;

    /**
     * 平均评分
     */
    private BigDecimal avgRating;

    /**
     * 评价时间
     */
    private LocalDateTime gmtCreated;

    /**
     * 部门id
     */
    private String departmentId;

    /**
     * 归档状态
     */
    private Integer archived;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String tagName;
}
