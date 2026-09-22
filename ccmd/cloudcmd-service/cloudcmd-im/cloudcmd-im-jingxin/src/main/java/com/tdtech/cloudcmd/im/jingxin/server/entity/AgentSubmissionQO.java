package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@ToString
@Schema(description = "申请查询对象")
public class AgentSubmissionQO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "申请人ID")
    private Long fromId;

    @Schema(description = "申请人")
    private String fromName;

    @Schema(description = "申请人部门")
    private Long fromDepId;

    @Schema(description = "申请人部门")
    private String fromDepName;

    @Schema(description = "审批人ID")
    private Long toId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请开始时间")
    private LocalDateTime createTimeBegin;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请结束时间")
    private LocalDateTime createTimeEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批开始时间")
    private LocalDateTime submissionTimeBegin;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批结束时间")
    private LocalDateTime submissionTimeEnd;

    @Schema(description = "申请状态")
    private Integer status;

    @Schema(description = "是否在有效期内")
    private Boolean available;

    @Schema(description = "资源类型")
    private Integer resourceType;

}
