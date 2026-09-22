package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Data
@Schema(description = "普通群组")
public class CreateGroupCOV2 implements Serializable {
    @Schema(description = "创建人ID")
    private String ownerId;
    @Schema(description = "创建人姓名")
    private String ownerName;
    @Schema(description = "创建人身份证号")
    private String idCard;
    @Schema(description = "部门ID")
    private Long departmentId;
    @Schema(description = "部门编码")
    private String departmentCode;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "ID Path")
    private String departmentFullPath;
    @Schema(description = "创建时间",hidden = true)
    private Date createTime;
    @Schema(description = "更新时间",hidden = true)
    private Date updateTime;
    @Schema(description = "群组名称")
    private String groupName;
    @Schema(description = "群组人员ID集合")
    private List<Long> userIds;
    @Schema(description = "身份证ID列表")
    private List<String> idCards;
    @Schema(description = "来源：1 =一键建群，3=自定义建群，5=一键调度")
    private Integer source;
}
