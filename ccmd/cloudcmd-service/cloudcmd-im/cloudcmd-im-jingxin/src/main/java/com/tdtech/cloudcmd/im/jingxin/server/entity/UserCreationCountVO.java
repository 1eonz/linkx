package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class UserCreationCountVO {
    /**
     * 用户名
     */
    private String  userName;
    /**
     * 部门名称
     */
    private String  departmentName;
    /**
     * 数量
     */
    private Integer totalCount;
    /**
     * 用户id
     */
    private String userId;

}
