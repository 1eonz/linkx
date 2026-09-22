package com.tdtech.cloudcmd.im.openapi.controller.entity;

import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "开放API群组视图对象")
public class GroupOpenApiVO extends GroupVO {

    @Schema(description = "群组标签列表")
    private List<GroupTagVO> tags;

}
