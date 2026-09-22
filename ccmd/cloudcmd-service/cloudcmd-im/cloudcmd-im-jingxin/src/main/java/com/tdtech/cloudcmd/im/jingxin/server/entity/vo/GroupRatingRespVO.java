package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 群组评价创建响应对象
 */
@Data
public class GroupRatingRespVO {
    /**
     * 群组ID
     */
    private Long groupId;
    /**
     * 协同岗评价列表
     */
    private List<CoopUserRatingRespItemVO> ratings;
}
