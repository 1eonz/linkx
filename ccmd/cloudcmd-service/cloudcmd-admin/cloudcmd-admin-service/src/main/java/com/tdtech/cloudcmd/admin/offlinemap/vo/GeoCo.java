package com.tdtech.cloudcmd.admin.offlinemap.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GeoCo implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 地图编码
     */
    @NotBlank(message = "Illegal geocode")
    private String geocode;

    /**
     * 地理逆编码
     */
    @NotBlank(message = "Illegal inversecode")
    private String inversecode;

    /**
     * POI搜索
     */
    @NotBlank(message = "Illegal poi")
    private String poi;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 类型
     */
    private String type;

    /**
     * 行政区域id
     */
    private Long nodeId;
}
