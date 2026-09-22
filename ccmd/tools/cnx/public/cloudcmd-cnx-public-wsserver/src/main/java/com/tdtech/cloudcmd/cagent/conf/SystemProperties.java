package com.tdtech.cloudcmd.cagent.conf;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class SystemProperties {
    private final String ip = InetAddress.getLocalHost().getHostAddress();
    @Value("${server.port}")
    private Integer port;
    @Value("${spring.application.name}")
    private String appName;

    public SystemProperties() throws UnknownHostException {}

    public String getIpPort() {
        return ip + ":" + port;
    }
}
