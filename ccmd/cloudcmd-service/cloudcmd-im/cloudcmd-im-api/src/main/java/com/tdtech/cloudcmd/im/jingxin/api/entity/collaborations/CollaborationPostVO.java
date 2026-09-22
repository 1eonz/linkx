package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CollaborationPostVO implements Serializable {

    private Long id;
    /**
     * 协同岗名称
     */
    private String postName;

    private String fileId;
    /**
     * 图标URL
     */
    private String iconUrl;
    /**
     * 所属组织ID
     */
    private Long orgId;

    private String orgCode;
    /**
     * 所属组织名称
     */
    private String orgName;
    /**
     * 关联人员IDs
     */
    private String relatedUserIds;
    /**
     * 关联人员名称
     */
    private String relatedUserNames;
    /**
     * 操作类型 1为创建，2为修改
     */
    private Integer operationType;
    /**
     * 数据来源：0-平台操作，1-im同步
     */
    private Integer source;
    /**
     * 操作人ID
     */
    private Long operatorId;
    /**
     * 操作人姓名
     */
    private String operatorName;
    /**
     * 操作时间
     */
    private Date operateTime;
    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 0 普通 1 1：14E
     */
    private Integer type;

}
