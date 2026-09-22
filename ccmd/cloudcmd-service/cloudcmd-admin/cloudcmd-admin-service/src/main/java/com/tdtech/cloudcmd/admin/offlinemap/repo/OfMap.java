package com.tdtech.cloudcmd.admin.offlinemap.repo;

import java.io.Serializable;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@TableName("tb_map")
public class OfMap implements Serializable {
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
     * 状态,0-离线 1-在线
     */
    private Integer status;

    /**
     * 设备类型,0-栅格，1-矢量
     */
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
    private Integer displayType;
}
