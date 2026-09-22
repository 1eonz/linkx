package com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket;

import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "警单查询对象")
public class PoliceTicketQO extends PoliceTicket  implements Serializable {

    @Schema(description = "群组ID")
    private Long groupId;

    @Schema(description = "是否只看绑定的，groupId有值时才生效，默认全部")
    private Integer bindFlag;

    @Schema(description = "协同岗ID，查协同岗关联的警单")
    private Long postId;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date startTime;

    @Schema(description = "时间范围查询")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Date endTime;

    @Schema(description = "群组类型，判断是普通群组，还是协同群组,1、普通群组，2、协同群组")
    @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS)
    private Integer groupType;

}
