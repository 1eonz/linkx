package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
public class CollabsTaskListReqCO {
    private Integer type;
    private String keywords;
    private Integer page;
    private Integer pageSize;
    private Long userId;
    private Long postId;
    private String postIds;

    public CollabsTaskListReqCO(Integer type, String keywords, Integer page, Integer pageSize,Long userId,Long postId, String postIds) {
        this.keywords = keywords;
        this.page = page;
        this.type = type;
        this.pageSize = pageSize;
        this.userId = userId;
        this.postId=postId;
        this.postIds = postIds;
    }
}
