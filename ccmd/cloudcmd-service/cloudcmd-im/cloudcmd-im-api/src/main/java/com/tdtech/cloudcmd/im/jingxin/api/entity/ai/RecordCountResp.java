package com.tdtech.cloudcmd.im.jingxin.api.entity.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "记录统计响应对象")
public class RecordCountResp implements Serializable {

    /**
     * agent group
     */
    @Schema(description = "智能体ID")
    private Long agentId;
    @Schema(description = "智能体列表")
    private String agentName;
    @Schema(description = "分类列表")
    private List<Category> categorys;

    /**
     * user group
     */
    @Schema(description = "用户名")
    private String userName;
    @Schema(description = "身份证号码")
    private String identityCardNumber;
    @Schema(description = "部门编码")
    private String departmentCode;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "部门ID")
    private Long departmentId;

    /**
     * date group
     */
    @JsonFormat(pattern = DateFormatUtil.YYYY_MM_DD)
    @Schema(description = "日期", example = "2023-01-01")
    private Date date;

    @Schema(description = "统计数量")
    private Long count;
}

