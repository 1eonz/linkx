package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class UserPolicyReq {

    /**
     * 发布对象类型。0-全局 1-指定对象，默认为0 (必选)
     */
    private Integer type;
    /**
     * 白名单可见部门ID列表 (非必选)
     */
    private String whiteDepartmentIds;
    /**
     * 白名单可见警种ID列表 (非必选)
     */
    private String whiteUserTypeIds;
    /**
     * 白名单可见人员ID列表 (非必选)
     */
    private String whiteUserIds;
    /**
     * 红名单可见部门ID列表 (非必选)
     */
    private String redDepartmentIds;
    /**
     * 红名单可见警种ID列表 (非必选)
     */
    private String redUserTypeIds;
    /**
     * 红名单可见人员ID列表 (非必选)
     */
    private String redUserIds;
}
