package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "协同群组关联任务")
public class TasksGroupVO extends Tasks {

    @Schema(description = "是否绑定当前群组，0 未绑定 1 绑定")
    private Integer bindFlag;
}
