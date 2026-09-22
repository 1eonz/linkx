package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.icp.proxy.entity.DynamicGroupMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Valid
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "动态群组信息")
public class DynamicGroupVO {

    @NotBlank
    @Schema(description = "群组ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "group001")
    private String groupId;

    @NotBlank
    @Schema(description = "群组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "group001")
    private String groupName;

    @NotBlank
    @Schema(description = "群主ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "group001")
    private String ownerId;

    @NotNull
    @Schema(description = "群组成员列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DynamicGroupMember> members;
}
