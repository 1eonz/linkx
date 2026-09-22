package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@ToString(callSuper = true)
@Schema(description = "协同群组关联任务查询对象")
public class TasksGroupQO {

    @Schema(description = "搜索关键字")
    private String keywords;

    @Schema(description = "群组ID")
    private Long groupId;

    @Schema(description = "是否只看绑定的，groupId有值时才生效，默认全部")
    private Integer bindFlag;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date startTime;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date endTime;

}
