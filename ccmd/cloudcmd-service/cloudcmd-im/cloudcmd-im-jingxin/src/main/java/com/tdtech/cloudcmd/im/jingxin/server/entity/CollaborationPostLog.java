package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_collaboration_post_log")
@Data
public class CollaborationPostLog implements Serializable {
    private Long id;

    /**
     * 协同岗名称
     */
    private String postName;

    /**
     * 所属组织ID
     */
    private Long orgId;

    /**
     * 所属组织编码
     */
    private String orgCode;

    /**
     * 所属组织名称
     */
    private String orgName;

    /**
     * 关联人员IDS
     */
    private String relatedUserIds;

    /**
     * 关联人员名称
     */
    private String relatedUserNames;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作类型 1为创建，2为修改
     */
    private Integer operationType;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 操作时间
     */
    private Date operateTime;

}