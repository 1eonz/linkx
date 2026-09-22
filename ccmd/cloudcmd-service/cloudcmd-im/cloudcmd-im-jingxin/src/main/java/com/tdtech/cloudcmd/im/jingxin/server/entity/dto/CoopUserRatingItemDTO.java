package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 协同岗评价项请求对象
 */
@Data
public class CoopUserRatingItemDTO {

    /**
     * 被评价协同岗ID
     */
    @NotNull(message = "被评价协同岗ID不能为空")
    private Long coopUserId;

    /**
     * 被评价协同岗名称
     */
    @NotBlank(message = "被评价协同岗名称不能为空")
    private String coopUserName;

    /**
     * 评价详情
     */
    @NotNull(message = "评价详情不能为空")
    @Valid
    private RatingDetailsDTO ratingDetails;
}
