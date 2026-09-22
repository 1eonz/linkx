package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 评价详情请求对象
 */
@Data
public class RatingDetailsDTO {

    /**
     * 维度评分列表
     */
    @NotEmpty(message = "维度评分不能为空")
    @Valid
    private List<RatingDimensionItemDTO> dimensions;

    /**
     * 评价评论
     */
    @Size(max = 500, message = "评论长度不能超过500字符")
    private String comment;
}
