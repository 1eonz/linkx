package com.tdtech.cloudcmd.im.jingxin.server.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群组评价状态响应对象
 */
@Data
public class GroupRatingStatusRespVO {
    /**
     * 是否已完成所有评价
     */
    private Boolean completed;
    /**
     * 协同岗总数
     */
    private Long totalCoopUsers;
    /**
     * 已评价协同岗数
     */
    private Long ratedCoopUsers;
    /**
     * 完成评价时间
     */
    private LocalDateTime gmtCompleted;
}
