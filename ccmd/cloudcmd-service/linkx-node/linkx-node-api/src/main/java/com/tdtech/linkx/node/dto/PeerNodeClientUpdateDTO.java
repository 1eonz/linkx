package com.tdtech.linkx.node.dto;

import lombok.Data;

/**
 * 更新客户端节点DTO
 */
@Data
public class PeerNodeClientUpdateDTO {

    private String name;

    private String remark;

    private Integer grant;

    private Long expiredIn;

    private String tag;
}
