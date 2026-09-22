package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@Schema(description = "群组视图对象")
public class GroupVO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "群主ID")
    private String ownerId;
    
    @Schema(description = "群主名称")
    private String ownerName;

    /**
     * 名称是ID，实际上是组织编码。。狗屎玩意
     */
    @Schema(description = "部门ID")
    private String departmentId;
    
    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
    
    @Schema(description = "用户ID列表")
    private String userIds;
    
    @Schema(description = "用户名列表")
    private String userNames;

    @Schema(description = "群组ID")
    private Long groupId;

    @Schema(description = "群组名称")
    private String groupName;

    /**
     * 位置：经纬度，格式：经度,纬度
     */
    @Schema(description = "位置坐标，格式：经度,纬度")
    private String location;

    /**
     * 群头像
     */
    @Schema(description = "群头像")
    private String avatar;
    
    /**
     * 群头像图片
     */
    @Schema(description = "群头像图片")
    private String avatarImg;

}
