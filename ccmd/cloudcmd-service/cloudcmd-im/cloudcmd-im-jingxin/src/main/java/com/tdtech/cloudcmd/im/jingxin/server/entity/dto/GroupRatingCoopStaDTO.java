package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;


/**
 * 协同岗统计评分DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingCoopStaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 各维度评价统计
     */
    private Map<Long, GroupRatingDimensionDTO> dimensionMap;

    /**
     * 平均评分
     */
    private BigDecimal avgRating;
}
