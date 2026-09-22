package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

/**
 * 评价维度项响应对象
 */
@Data
public class RatingDimensionItemVO {
    /**
     * 维度ID
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
     * 评分
     */
    private Integer score;
}
