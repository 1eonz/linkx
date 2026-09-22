package com.tdtech.cloudcmd.icp.proxy.conf;

import cloudcmd.service.rpc.IcpConfigRpcService;
import com.tdtech.cloudcmd.icp.proxy.entity.IcpConfig;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class IcpProperties {

    private static final String URI_FORMAT_HTTP = "https://%s:%s";

    private static final String URI_FORMAT_WSS = "wss://%s:%s%s";

    @DubboReference
    private IcpConfigRpcService icpConfigRpcService;

    @SneakyThrows
    public URI getWsUri() {
        IcpConfig icpConfig = getIcpConfig();
        return new URI(String.format(URI_FORMAT_WSS, icpConfig.getIp(), icpConfig.getPort(), icpConfig.getWssUrl()));
    }

    @SneakyThrows
    public URI getUnifiLoginURI() {
        IcpConfig icpConfig = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, icpConfig.getIp(), icpConfig.getPort());
        return new URI(
            String.format("%s/sdkserver/v1/register/%s/unifilogin", icpUrl, icpConfig.getUsername()));
    }

    @SneakyThrows
    public URI getHeartbeatURI() {
        IcpConfig icpConfig = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, icpConfig.getIp(), icpConfig.getPort());
        return new URI(String.format("%s/sdkserver/v1/heartbeat/%s", icpUrl, icpConfig.getUsername()));
    }

    @SneakyThrows
    public URI getGisSubURI() {
        IcpConfig icpConfig = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, icpConfig.getIp(), icpConfig.getPort());
        return new URI(String.format("%s/sdkserver/v1/gis/%s/sub", icpUrl, icpConfig.getUsername()));
    }

    @SneakyThrows
    public URI getOnlineStatusSubURI() {
        IcpConfig icpConfig = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, icpConfig.getIp(), icpConfig.getPort());
        return new URI(String.format("%s/sdkserver/v1/person/%s/", icpUrl, icpConfig.getUsername()));
    }

    @SneakyThrows
    public URI getDepartmentURI() {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(
            String.format("%s/sdkserver/v1/querylist/%s/department", icpUrl, config.getUsername()));
    }

    @SneakyThrows
    public URI getCameraLevelURI() {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(
            String.format("%s/sdkserver/v1/querylist/%s/cameralevel", icpUrl, config.getUsername()));
    }

    @SneakyThrows
    public URI getCameraByLevelURI() {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(String.format("%s/sdkserver/v1/querylist/%s/querylevelcameras", icpUrl, config.getUsername()));
    }

    @SneakyThrows
    public URI getCameraURI(@NotNull Integer offset, @NotNull Integer limit) {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(String.format("%s/sdkserver/v2/%s/camera?offset=%d&limit=%d", icpUrl, config.getUsername(), offset, limit));
    }

    @SneakyThrows
    public URI getCameraByIsdnURI(String isdn) {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(String.format("%s/sdkserver/v1/queryattribute/%s/camera/%s", icpUrl, config.getUsername(), isdn));
    }

    /**
     * 用于根据isdn查询用户信息(场景：1. 收到ws创建用户消息；2. 登录时查询代理用户)
     * @return uri
     */
    @SneakyThrows
    public URI getUserByIsdnURI(String isdn) {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        return new URI(String.format("%s/sdkserver/v1/queryattribute/%s/user/%s", icpUrl, config.getUsername(), isdn));
    }

    @SneakyThrows
    public URI getUserByDepartmentURI(@NotNull Integer offset, @NotNull Integer limit, String departmentId, String category) {
        IcpConfig config = getIcpConfig();
        String icpUrl = String.format(URI_FORMAT_HTTP, config.getIp(), config.getPort());
        var urlString = String.format("%s/sdkserver/v2/%s/user?offset=%d&limit=%d", icpUrl, config.getUsername(), offset, limit);
        if (departmentId != null && !departmentId.isBlank()) {
            urlString += "&departmentid=" + departmentId;
        }
        if (category != null && !category.isBlank()) {
            urlString += "&category=" + category;
        }
        return new URI(urlString);
    }

    public String getIcpPwd() {
        IcpConfig icpConfig = getIcpConfig();
        return icpConfig.getPassword();
    }

    public String getIcpAccount() {
        IcpConfig icpConfig = getIcpConfig();
        return icpConfig.getUsername();
    }

    private IcpConfig getIcpConfig() {
        IcpConfig icpConfig = SyncUtil.getIcpConfig();
        if (icpConfig == null || StringUtils.isNullBlank(icpConfig.getIp()) ||
                icpConfig.getPort() == null||
                StringUtils.isNullBlank(icpConfig.getUsername()) ||
                StringUtils.isNullBlank(icpConfig.getPassword()) ||
                StringUtils.isNullBlank(icpConfig.getWssUrl())){
            icpConfig = new IcpConfig();
            BeanUtils.copyProperties(icpConfigRpcService.getIcpConfig(), icpConfig);
            SyncUtil.setIcpConfig(icpConfig);
        }

        return icpConfig;
    }
}
