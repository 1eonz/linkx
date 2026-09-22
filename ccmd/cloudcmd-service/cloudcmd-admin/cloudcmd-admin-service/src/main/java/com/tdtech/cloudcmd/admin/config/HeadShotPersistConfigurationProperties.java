package com.tdtech.cloudcmd.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudcmd.executor.head-shot")
public class HeadShotPersistConfigurationProperties {

    private String tempDir = "/home/linkx/headshot/temp/";
    private String persistDir = "/home/linkx/headshot/persist/";
    private String persistUriPrefix = "/static/persist/";
    private String tmpUriPrefix = "/static/temp/";

}
