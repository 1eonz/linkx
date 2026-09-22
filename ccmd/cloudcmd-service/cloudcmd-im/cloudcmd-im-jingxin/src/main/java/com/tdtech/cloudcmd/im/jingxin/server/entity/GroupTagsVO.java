package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.List;

/**
 * @author: S063874
 * @date: 2026-05-14 10:47
 */
@Data
public class GroupTagsVO {

    private Long groupId;

    private List<Long> tagIds;

    private Long userId;
}
