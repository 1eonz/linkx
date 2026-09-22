package com.tdtech.linkx.node.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 通用分页请求DTO
 */
@Data
public class PageReqDTO {

    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    private Integer pageSize = 10;
}
