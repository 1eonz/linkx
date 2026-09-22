package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 群组评价创建请求对象
 */
@Data
public class GroupRatingReqDTO {

    /**
     * 协同岗评价列表
     */
    @NotEmpty(message = "评价列表不能为空")
    @Valid
    private List<CoopUserRatingItemDTO> ratings;
}
