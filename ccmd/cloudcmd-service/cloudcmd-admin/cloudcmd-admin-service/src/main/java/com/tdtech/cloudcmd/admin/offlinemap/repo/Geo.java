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
@TableName("tb_geo")
public class Geo implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 地图编码
     */
    private String geocode;

    /**
     * 地理逆编码
     */
    private String inversecode;

    /**
     * POI搜索
     */
    private String poi;

    /**
     * 创建时间
     */
    private Date created;
}
