package com.tdtech.cloudcmd.admin.offlinemap.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MapQo implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 地图名称
     */
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态 0-离线 1-在线
     */
    private Integer status;

    /**
     * 设备类型，0-栅格，1-矢量
     */
    private Integer type;

    /**
     * 激活状态
     */
    private Integer activation;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 当前页数
     */
    private Integer pageNo;

    /**
     * 每页显示数据条数
     */
    private Integer pageSize;

    /**
     * 支持地图前端类型，0-ICC，1-CAPP
     */
    private Integer displayType;

    /**
     * 地图配置
     */
    private String configuration;

    /**
     * 地图类型
     */
    private String mapType;
}
