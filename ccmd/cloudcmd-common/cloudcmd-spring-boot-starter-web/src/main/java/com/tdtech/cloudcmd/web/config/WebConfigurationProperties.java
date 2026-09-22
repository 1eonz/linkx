package com.tdtech.cloudcmd.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudcmd.web")
public class WebConfigurationProperties {

    private Boolean wrapRequest = false;

}
