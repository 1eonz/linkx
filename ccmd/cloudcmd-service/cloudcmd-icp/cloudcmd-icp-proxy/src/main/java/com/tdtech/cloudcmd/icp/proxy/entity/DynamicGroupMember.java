package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("tb_dynamic_group_member")
@Schema(description = "动态群组成员信息")
public class DynamicGroupMember {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "群组ID", example = "group001")
    private String groupId;

    @Schema(description = "群组名称", example = "group001")
    private String groupName;

    @Schema(description = "用户ID", example = "user001")
    private String userId;

    @Schema(description = "用户名", example = "张三")
    private String userName;

    @Schema(description = "ISDN号码", example = "123456789")
    private String isdn;

    @Schema(description = "头像", example = "data:image/png;base64,iVBORXXXX")
    private String tumbAvatar;

    @Schema(description = "群主", example = "1234")
    private String ownerId;
}
