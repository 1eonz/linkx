package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * 群组标签统计评分DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingTagStaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价人类型
     */
    private Integer raterType;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 群组ID列表
     */
    private List<GroupRatingStaDTO> groupRatingStaList;

    /**
     * 各维度评价统计
     */
    private Map<Long, GroupRatingDimensionDTO> dimensionMap;

    /**
     * 平均评分
     */
    private BigDecimal avgRating;
}
