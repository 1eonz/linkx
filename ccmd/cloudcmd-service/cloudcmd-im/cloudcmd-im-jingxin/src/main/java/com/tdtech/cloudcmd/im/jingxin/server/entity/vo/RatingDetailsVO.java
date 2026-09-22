package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 评价详情响应对象
 */
@Data
public class RatingDetailsVO {
    /**
     * 维度评分列表
     */
    private List<RatingDimensionItemVO> dimensions;
    /**
     * 评价评论
     */
    private String comment;
}
