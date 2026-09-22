package com.tdtech.cloudcmd.im.openapi.controller.entity;

import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Schema(description = "警单查询对象")
public class OpenApiPoliceTicketQO {

    @Schema(description = "群组ID，跟postId条件互斥")
    private Long groupId;

    @Schema(description = "是否只看绑定的，groupId有值时才生效，默认全部")
    private Integer bindFlag;

    @Schema(description = "协同岗ID，查协同岗关联的警单，跟groupId条件互斥")
    private Long postId;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date startTime;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date endTime;

    // 名称
    @NotNull
    @Schema(description = "名称,模糊搜索")
    private String name;

    // 单号
    @NotNull
    @Schema(description = "单号,模糊搜索")
    private String code;

    @NotNull
    @Schema(description = "内容,模糊搜索")
    private String content;

}
