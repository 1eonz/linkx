package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
public class CollabsTaskResponseListReqCO {
    private Integer page;
    private Integer pageSize;
    private Long userId;

    public CollabsTaskResponseListReqCO( Integer page, Integer pageSize,Long userId) {
        this.page = page;
        this.pageSize = pageSize;
        this.userId = userId;
    }
}
