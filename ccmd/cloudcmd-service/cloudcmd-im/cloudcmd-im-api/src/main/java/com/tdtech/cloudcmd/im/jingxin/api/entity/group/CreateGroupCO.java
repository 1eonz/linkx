package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
@Schema(description = "一键建群")
public class CreateGroupCO  implements Serializable {
    /**
     * 一键建群时，传入标签的id集合，职能建群时，选择协同岗集合ids
     */
    @NotNull
    @Schema(description = "标签ID数组或者协同岗id")
    private List<String> ids;
    @NotBlank
    @Schema(description = "创建人ID")
    private String ownerId;

    @Schema(description = "创建人姓名")
    private String ownerName;

    @Schema(description = "创建人身份证号")
    private String idCard;
    @NotNull
    @Schema(description = "部门ID")
    private Long departmentId;
    @NotBlank
    @Schema(description = "部门编码")
    private String departmentCode;
    @NotBlank
    @Schema(description = "部门名称")
    private String departmentName;
    @NotBlank
    @Schema(description = "ID Path")
    private String departmentFullPath;

    @Schema(description = "创建时间",hidden = true)
    private Date createTime;

    @Schema(description = "更新时间",hidden = true)
    private Date updateTime;

    // 位置：经纬度，格式：经度,纬度
    @Schema(description = "位置：经纬度，格式：经度,纬度")
    private String location;

    // 位置：经纬度，格式：经度,纬度
    @Schema(description = "是否自动绑定眼镜，0或空：否，1：是")
    private String bindGlasses;
    @Schema(description = "群组名称")
    private String groupName;

    @Schema(description = "选择标签的人员id集合")
    private List<Long> labelUserIds;

    @Schema(description = "选择标签的人员姓名集合")
    private List<String> labelUserNames;

    @Schema(description = "职能建群时选择的标签id")
    private List<Long> labelIds;

    @Schema(description = "群组人员id集合")
    private List<Long> userIds;

    @Schema(description = "协同岗ID集合")
    private List<Long> cPostIds;

    @Schema(description = "额外选择的协同岗ID集合（非标签关联的协同岗，可含跨节点协同岗）")
    private List<Long> coopUserIds;

    @Schema(description = "来源：1 =一键建群，3=自定义建群，5=一键调度")
    private Integer source = 1;

}
