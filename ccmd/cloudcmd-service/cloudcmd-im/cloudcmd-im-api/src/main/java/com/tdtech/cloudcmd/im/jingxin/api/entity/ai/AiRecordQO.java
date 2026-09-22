package com.tdtech.cloudcmd.im.jingxin.api.entity.ai;

import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@Valid
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "AI记录查询对象")
public class AiRecordQO implements Serializable {

    @Schema(description = "部门编码")
    private String departmentCode;

    @NotNull
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD)
    @Schema(description = "开始时间", required = true, example = "2023-01-01")
    private Date startTime;

    @NotNull
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD)
    @Schema(description = "结束时间", required = true, example = "2023-12-31")
    private Date endTime;

    @Schema(description = "人员姓名")
    private String personName;

    @Schema(description = "分类")
    private String category;

}