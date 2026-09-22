package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class OpenApiCreateGroupCOV3 implements Serializable {

    @Schema(description = "1:一键建群，3:自定义建群，5:一键调度")
    private Integer subType;

    @Schema(description = "1:普通群组，2:协同群组")
    @NotNull
    @Range(min = 1, max = 2, message = "type仅支持1(普通群组)或2(协同群组)")
    private Integer type;

    // 260907拓展支持协同岗id 并且支持混传
    @Schema(description = "用户ID列表")
    private List<Long> userIds;

    @Schema(description = "身份证列表")
    private List<String> idCards;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "群组名称")
    private String groupName;
}