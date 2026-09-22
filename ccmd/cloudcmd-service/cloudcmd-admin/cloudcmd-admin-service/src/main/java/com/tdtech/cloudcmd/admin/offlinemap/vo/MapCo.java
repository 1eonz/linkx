package com.tdtech.cloudcmd.admin.offlinemap.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MapCo implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 地图名称
     */
    @NotBlank(message = "Illegal name")
    private String name;

    /**
     * 图标
     */
    @NotBlank(message = "Illegal icon")
    private String icon;

    /**
     * 状态,0-离线 1-在线
     */
    private Integer status;

    /**
     * 设备类型,0-栅格，1-矢量
     */
    @NotNull(message = "type cannot equal null")
    private Integer type;

    /**
     * 激活状态,0-未激活，1-激活
     */
    private Integer activation;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 地图配置
     */
    private String configuration;

    /**
     * 地图类型
     */
    private String mapType;

    /**
     * 支持地图前端类型，0-ICC，1-CAPP
     */
    @NotNull(message = "displayType cannot equal null")
    private Integer displayType;
}
