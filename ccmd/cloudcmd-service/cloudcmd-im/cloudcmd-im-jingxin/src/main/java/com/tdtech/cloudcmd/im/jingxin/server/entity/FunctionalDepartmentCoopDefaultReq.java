package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.List;

/**
 * 部门协同岗用户关联默认表请求参数
 */
@Data
public class FunctionalDepartmentCoopDefaultReq {

    /**
     * 协同岗用户ID
     */
    private Long userId;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 协同岗用户ID列表（批量操作）
     */
    private List<Long> userIds;
}