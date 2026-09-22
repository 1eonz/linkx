package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
public class CollaborationReplyTotalVO implements Serializable {
    /**
     * 部门编码
     */
    private String departmentCode;
    /**
     * 部门名称
     */
    private String departmentName;
    /**
     * 总回复数
     */
    private Integer total;
}
