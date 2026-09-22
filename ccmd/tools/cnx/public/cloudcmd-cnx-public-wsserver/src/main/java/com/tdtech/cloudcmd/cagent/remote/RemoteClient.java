package com.tdtech.cloudcmd.cagent.remote;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.UserInfo;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.util.json.JsonArray;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RemoteClient {

    @Resource
    private HttpClient httpClient;
    @Resource
    private RemoteConfigurationProperties remoteConfigurationProperties;

    @SneakyThrows
    public List<Organization> allOrgs() {
        var uri = new URI(remoteConfigurationProperties.getSchema() + "://" + remoteConfigurationProperties.getHost()
            + remoteConfigurationProperties.getOrgPath());
        var jsonStr = httpClient.getJson(uri, remoteConfigurationProperties.getHeaders(), String.class);
        log.info("get org:{}", jsonStr);
        return JsonUtil.parseArrayJson(jsonStr, Organization.class);
    }

    @SneakyThrows
    public UserInfo auth(String token) {
        var uri = new URI(remoteConfigurationProperties.getSchema() + "://" + remoteConfigurationProperties.getHost()
            + remoteConfigurationProperties.getAuthPath());
        var userInfoStr = httpClient.postJson(uri, remoteConfigurationProperties.getHeaders(), token, String.class);
        log.info("auth:{}", userInfoStr);
        var jsonObject = JsonUtil.parseJson(userInfoStr);
        if (jsonObject == null) {
            log.warn("auth error,token:{} resp:{}", token, userInfoStr);
            return null;
        }
        jsonObject.put("createTime", null);
        jsonObject.put("userPeriod", null);
        return JsonUtil.convert(jsonObject, UserInfo.class);
    }

    @SneakyThrows
    public JsonArray pullMsg(String channel) {
        var uri = new URI(remoteConfigurationProperties.getSchema() + "://" + remoteConfigurationProperties.getHost()
            + remoteConfigurationProperties.getPullMsgPath() + "/" + channel);
        var msgStr = httpClient.getJson(uri, remoteConfigurationProperties.getHeaders(), String.class);
        if (!Objects.equals(msgStr, "[]")) {
            log.info("channel：{} pull:{}", channel, msgStr);
        }
        return JsonUtil.parseArrayJson(msgStr);
    }
}
