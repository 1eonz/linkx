package com.tdtech.cloudcmd.icp.proxy.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_icp_config")
@Schema(description = "融合通信平台配置信息")
public class IcpConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 支持IPv4和IPv6
     */
    @Schema(description = "服务器IP地址")
    private String ip;

    @Schema(description = "端口号")
    private Integer port;

    @Schema(description = "网关代理用户")
    private String username;

    @Schema(description = "网关代理用户密码")
    private String password;

    private String wssUrl;

    @Schema(description = "部门根节点")
    private String departmentId;

    @Schema(description = "部门根节点名称")
    private String departmentName;

    @Schema(description = "摄像头层级根节点")
    private String cameraLevelId;

    @Schema(description = "摄像头层级根节点名称")
    private String cameraLevelName;

    /**
     * 协议类型：1-SSH，2-RDP，3-Telnet，4-VNC，5-HTTP，6-其他
     */
    @Schema(description = "协议类型")
    private Integer protocol;

    /**
     * 环境类型：0-生产，1-测试，2-开发，3-预发布
     */
    @Schema(description = "环境类型")
    private Integer environment;

    /**
     * 状态：0-禁用，1-启用
     */
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注信息")
    private String remark;

}