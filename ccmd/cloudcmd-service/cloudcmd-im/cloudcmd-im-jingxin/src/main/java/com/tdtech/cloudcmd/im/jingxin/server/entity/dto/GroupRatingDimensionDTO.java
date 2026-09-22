package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 协同岗统计评分DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingDimensionDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 维度id
     */
    private Long id;

    /**
     * 维度编码
     */
    private String code;

    /**
     * 维度名称
     */
    private String name;

    /**
     * 评分总和
     */
    private BigDecimal ratingSum = new BigDecimal(0);


    /**
     * 评分条数
     */
    private Integer ratingCount = 0;

    /**
     * 平均分
     */
    private BigDecimal avgScore;
}
