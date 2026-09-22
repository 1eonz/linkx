package com.tdtech.cloudcmd.cagent.remote;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cnx.public.remote")
public class RemoteConfigurationProperties {
    private String host;
    private String schema;
    private String pullMsgPath;
    private String orgPath;
    private String authPath;
    private Map<String, String> headers;
}
