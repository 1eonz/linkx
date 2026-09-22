package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/11/17
 **/
@Data
public class GroupExtendsVO {

    /**
     * 归档的群组id
     */
    private Long groupId;

    /**
     * 归档的群组名称
     */
    private String groupName;

    /**
     * 归档的群组对应的标签名
     */
    private String tagName;

    /**
     * 关联的任务
     */
    private String taskName;
    /**
     * 所属部门
     */
    private String departmentName;

    /**
     * 归档时间
     */
    private String archivedTime;
    /**
     * 归档人
     */
    private String archiveUserName;
    /**
     * 归档位置
     */
    private String archivedFile;
    /**
     * 群组类型
     */
    private Integer groupType;
}
