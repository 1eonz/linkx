package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class CarouselPageReqVO  {

    @Schema(description = "标题")
    private String title;

    private Integer pageNum;

    private Integer pageSize;

}