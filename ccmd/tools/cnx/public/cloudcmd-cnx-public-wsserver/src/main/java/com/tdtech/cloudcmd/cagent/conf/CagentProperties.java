package com.tdtech.cloudcmd.cagent.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * cagent控制配置
 *
 * @author : mWX556161
 * @date : 2020-05-13 13:48
 */

@Data
@Component
@ConfigurationProperties(prefix = "cloudcmd.cagent")
public class CagentProperties {
    private Integer webPort = 8080;

    private Integer socketPort = 8081;

    private Integer rpcPort = 8082;

    private String websocketPath = "/cagent";

    private boolean ssl = true;

    private Integer heartbeatTimeout = 60;

    private Integer bossGroupThread = 10;

    private Integer workGroupThread = 100;

    private long removeDelay = 5000;

}
