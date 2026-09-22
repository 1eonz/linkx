package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 评价维度项请求对象
 */
@NoArgsConstructor
@Data
public class RatingDimensionItemDTO {

    /**
     * 维度ID
     */
    @NotNull(message = "维度ID不能为空")
    private Long id;

    /**
     * 维度编码
     */
    @NotBlank(message = "维度编码不能为空")
    private String code;

    /**
     * 维度名称
     */
    @NotBlank(message = "维度名称不能为空")
    private String name;

    /**
     * 评分
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能小于1")
    @Max(value = 5, message = "评分不能大于5")
    private Integer score;

    public RatingDimensionItemDTO(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}
