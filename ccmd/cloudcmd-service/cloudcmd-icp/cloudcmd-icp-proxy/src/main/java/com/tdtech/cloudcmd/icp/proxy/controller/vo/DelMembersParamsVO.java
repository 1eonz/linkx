package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import java.util.List;

/**
 * @author: S063874
 * @date: 2026-04-22 10:06
 */
@lombok.Data
public class DelMembersParamsVO {

    private List<String> userIds;

    private Integer isOwner;
}
