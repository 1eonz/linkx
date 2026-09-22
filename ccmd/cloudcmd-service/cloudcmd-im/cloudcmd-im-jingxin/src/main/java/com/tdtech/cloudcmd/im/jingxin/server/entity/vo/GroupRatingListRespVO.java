package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 群组评价列表响应对象
 */
@Data
public class GroupRatingListRespVO {
    /**
     * 评价记录列表
     */
    private List<GroupRatingListItemVO> records;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 当前页码
     */
    private Long pageNum;
    /**
     * 每页大小
     */
    private Long pageSize;
}
