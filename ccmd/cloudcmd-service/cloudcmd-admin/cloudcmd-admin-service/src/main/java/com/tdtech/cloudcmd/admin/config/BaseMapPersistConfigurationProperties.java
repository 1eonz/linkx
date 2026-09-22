package com.tdtech.cloudcmd.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudcmd.map.base-map")
public class BaseMapPersistConfigurationProperties {
    //上传图标
    private String mapiconDir = "/home/ics/offlinemap/data/mapicon/";
    private String mapiconUriPrefix = "/static/map/mapicon/";

    //上传底图
    private String basemapDir = "/home/ics/offlinemap/data/tilesets/";
    private String basemapUriPrefix = "/static/tilesets/";

    //上传行政区划
    private String divisionDir = "/home/ics/offlinemap/data/OSMB/";
    private String divisionUriPrefix = "/static/OSMB/";
}
