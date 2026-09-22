package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

@Data
@TableName("tb_police_ticket_type")
@Schema(description = "警单类型")
public class PoliceTicketType {

    @TableId
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "类型")
    private String tag;

    @Schema(description = "创建人")
    private String creator;
    
    private Long creatorId;

    @Schema(description = "时间")
    private Date gmtCreated;

}
