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
 * 群组评价统计评分DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingStaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价人类型
     */
    private Integer raterType;

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 各维度评价统计
     */
    private Map<Long, GroupRatingDimensionDTO> dimensionMap;

    /**
     * 平均评分
     */
    private BigDecimal avgRating;
}
