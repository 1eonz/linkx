package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class WsDepartmentNoticeMessage {
    // 批量变更消息的最后修改标识
    private Long etag;

    private List<WsDepartmentEvents> departmentEvents;
}
