package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.ResourceNotify;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_online_status")
@Schema(description = "在线状态信息")
public class OnlineStatus extends ResourceNotify.Status{

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

}
