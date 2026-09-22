package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/

@Data
public class UserReq {
    /**
     * 姓名 (必选)
     */
    private String name;
    /**
     *  头像原图 fileid (必选)
     */
    private String avatar;
    /**
     * 用户分类（数据字典定义）1-协同岗，默认为1 (非必选)
     */
    private Integer category;
    /**
     * 备注 (非必选)
     */
    private String remark;
    /**
     * 可见范围请求对象 (非必选)
     */
    private UserPolicyReq policy;
    /**
     * 绑定人员对象ID列表 (必选)
     */
    private List<Long> bindUserIds;

    /**
     * 所属部门ID (非必选)
     */
    private Long departmentId;
}