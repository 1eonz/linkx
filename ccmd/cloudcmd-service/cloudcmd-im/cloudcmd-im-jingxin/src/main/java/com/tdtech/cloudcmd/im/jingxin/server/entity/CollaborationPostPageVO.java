package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CollaborationPostPageVO extends CollaborationPost {
    private List<PoliceTicketType> policeTicketTypes;
    private Boolean selected = false;

    /**
     * 是否已分享到对端节点：0=未分享，1=已分享
     */
    private Integer isShared = 0;
}
