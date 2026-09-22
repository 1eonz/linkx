package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ClientConfigGroup;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.ImWsClient;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.React1Over1p4BClient;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ImClientSessionRefresh {

    private final ObjectProvider<ImHttpClient> httpClientObjectProvider;
    private final ObjectProvider<ImWsClient> wsClientObjectProvider;
    private final CachedImConfig cachedImConfig;
    private final React1Over1p4BClient react1Over1p4BClient;
    private final IOrganizationService organizationService;
    private final IDutyScheduleService dutyScheduleService;

    @Bean("onGlobalConfigChangeEvent")
    public Consumer<String> onGlobalConfigChangeEvent() {
        return msg -> {
            log.info("on global config change:{}", msg);
            var jsonObject = JsonUtil.parseJson(msg);
            var name = jsonObject.getString("name");
            cachedImConfig.clear();
            if (ClientConfigGroup.coopConfigNames().contains(name)) {
                httpClientObjectProvider.stream().filter(c -> Objects.equals(c.getName(), "coopImHttpClient"))
                    .forEach(ImHttpClient::deprecateToken);
                wsClientObjectProvider.stream().filter(c -> Objects.equals("coopImWsClient", c.getName()))
                    .forEach(ImWsClient::close);
                // im配置有变化，重新同步部门数据
                organizationService.reSync();
                // im配置有变化，重新同步人员数据
                dutyScheduleService.resetUserCache();
            }
            /* 群AI助手无法再通过全局变量进行配置，该部分内容无法再使用
            if (ClientConfigGroup.aiConfigNames().contains(name)) {
                httpClientObjectProvider.stream().filter(c -> Objects.equals(c.getName(), "groupAIImHttpClient"))
                    .forEach(ImHttpClient::deprecateToken);
                wsClientObjectProvider.stream().filter(c -> Objects.equals("groupAIWsClient", c.getName()))
                    .forEach(ImWsClient::close);
            }*/
            if (ClientConfigGroup.oneO1p4BConfigNames().contains(name)) {
                httpClientObjectProvider.stream().filter(c -> Objects.equals(c.getName(), "oneO1p4BImHttpClient"))
                        .forEach(ImHttpClient::deprecateToken);
                wsClientObjectProvider.stream().filter(c -> Objects.equals("oneO1p4BWsClient", c.getName()))
                        .forEach(ImWsClient::close);
            }
            if (ClientConfigGroup.warningConfigNames().contains(name)) {
                httpClientObjectProvider.stream().filter(c -> Objects.equals(c.getName(), "warningImHttpClient"))
                        .forEach(ImHttpClient::deprecateToken);
            }
            if (Arrays.asList(react1Over1p4BClient.getConfigNames()).contains(name)){
                react1Over1p4BClient.cleanSession();
            }
            // 修改了自动上下岗开关，触发重新订阅im的人员状态变化通知，预防警务im没有升级，造成没有订阅的情况
            if (ClientConfigGroup.autoOnlineOfflineConfigNames().contains(name)) {
                wsClientObjectProvider.stream().filter(c -> Objects.equals("coopImWsClient", c.getName()))
                        .forEach(ImWsClient::subscribeUserStateNoticeMessage);
            }
            // 修改了同步im部门信息开关，触发重新订阅im的部门变化通知，预防警务im没有升级，造成没有订阅的情况
            if (ClientConfigGroup.autoSyncDeptConfigNames().contains(name)) {
                wsClientObjectProvider.stream().filter(c -> Objects.equals("coopImWsClient", c.getName()))
                        .forEach(ImWsClient::subscribeDepartmentNoticeMessage);
            }
        };
    }

}
