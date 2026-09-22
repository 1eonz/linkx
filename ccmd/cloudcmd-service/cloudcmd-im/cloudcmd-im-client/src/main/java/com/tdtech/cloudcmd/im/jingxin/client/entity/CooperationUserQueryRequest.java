package com.tdtech.cloudcmd.im.jingxin.client.entity;
import lombok.Data;
/**
 * @author lsc
 * @date 2025/7/14
 **/


@Data
public class CooperationUserQueryRequest {

    /**
     * 部门 Code (非必选，与部门 ID 二者选其一)
     */
    private String departmentCode;
    /**
     * 部门 ID (非必选，与部门 Code 二者选其一)
     */
    private Long departmentId;
    /**
     * 是否包含所有下级部门 (0-不包括, 1-包括，默认为 0)
     */
    private Integer includeChildren;
    /**
     * 协同岗名称 (非必选)
     */
    private String cooperationUserName;
    /**
     * 协同岗ids (非必选)
     */
    private String cooperationUserIds;
    /**
     * 协同岗绑定人员名称 (非必选)
     */
    private String bindUserName;
    /**
     * 协同岗创建开始日期 (非必选)
     */
    private Long beginDate;
    /**
     * 协同岗创建结束日期 (非必选)
     */
    private Long endDate;
    /**
     * 排序类型 (0-正序, 1-倒序，默认为 1)
     */
    private Integer sortType;
    /**
     * 当前页码 (默认为 1)
     */
    private Integer pageNo;
    /**
     * 每页大小 (默认为 20)
     */
    private Integer pageSize;
}