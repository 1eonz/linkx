package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class CollaborationPostVO {

    @NotBlank(message = "协同岗名称不能为空")
    private String postName;

    private String fileId;

    private String iconUrl;

    private Long orgId;

    private String orgCode;

    private String orgName;

    private String relatedUserIds;

    private String relatedUserNames;

    private Integer operationType;

    private Integer source;

    private Long operatorId;

    private String operatorName;
    private String id;
    //0 普通 1 1：14E
    private Integer type;

    private List<Long> typeIds;
}
