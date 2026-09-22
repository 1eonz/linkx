package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
@ToString
@Schema(description = "申请创建对象")
public class AgentSubmissionCO {

    @Schema(description = "申请人ID")
    @NotNull
    private Long fromId;

    @NotNull
    @Schema(description = "申请资源")
    private List<AgentSubmission.SubmissionResource> resources;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "申请开始日期")
    @NotNull
    private LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "申请结束日期")
    @NotNull
    private LocalDate toDate;

    @Schema(description = "申请描述")
    @NotNull
    private String desc;

    @Schema(description = "扩展字段")
    private String ext;
}