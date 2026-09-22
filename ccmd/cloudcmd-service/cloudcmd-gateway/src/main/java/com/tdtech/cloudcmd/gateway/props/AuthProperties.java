package com.tdtech.cloudcmd.gateway.props;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import lombok.Getter;
import lombok.Setter;

/**
 * 权限过滤
 *
 * @author zWX523748
 */
@RefreshScope
@ConfigurationProperties("ccmd.secure")
@Getter
@Setter
public class AuthProperties {

    /**
     * 放行API集合
     */
    private List<String> skipUrl = new ArrayList<>();

    private List<String> tempUrl = new ArrayList<>();

}
