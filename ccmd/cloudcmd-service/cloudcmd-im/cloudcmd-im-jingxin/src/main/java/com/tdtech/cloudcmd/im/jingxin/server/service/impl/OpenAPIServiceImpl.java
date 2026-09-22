package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.IMVersionMsgReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ServerVersion;
import com.tdtech.cloudcmd.im.jingxin.server.service.OpenAPIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class OpenAPIServiceImpl implements OpenAPIService {

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final String PLATFORM_VERSION_URL = "http://msip-service.platform:8445/msip/oam/v1/version";

    @Resource
    private ImHttpClient imHttpClient;

    @Override
    public ServerVersion getServerVersion() {
        ServerVersion serverVersion = new ServerVersion();
        ServerVersion.Jx jx = new ServerVersion.Jx();
        ServerVersion.EAgent eAgent = new ServerVersion.EAgent();
        ServerVersion.Linkx linkx = new ServerVersion.Linkx();

        try {
            IMVersionMsgReq imVersionMsgReq = imHttpClient.getVersion();
            if (imVersionMsgReq != null) {
                jx.setAppVerion(imVersionMsgReq.getAppVersion());
                jx.setServiceVersion(imVersionMsgReq.getServiceVersion());
            }
        } catch (Exception e) {
            log.error("获取警信版本信息异常", e);
        }
        serverVersion.setJx(jx);

        try {
            HttpGet httpGet = new HttpGet(PLATFORM_VERSION_URL);
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpGet)) {
                log.info("请求版本接口响应状态：{}", response.getStatusLine());

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    log.warn("版本接口返回空响应");
                    return serverVersion;
                }

                String body = EntityUtils.toString(entity, "UTF-8");
                log.info("版本响应：{}", body);
                JSONArray jsonArray = JSONArray.parseArray(body);

                for (JSONObject obj : jsonArray.toJavaList(JSONObject.class)) {
                    String versionId = obj.getString("version_id");
                    String nameSpace = obj.getString("namespace");
                    if (nameSpace != null) {
                        //警务协调版本
                        if (nameSpace.equalsIgnoreCase("linkx")) {
                            linkx.setServiceVersion(versionId);
                        }
                        //MSIP版本
                        if (nameSpace.equalsIgnoreCase("platform")) {
                            eAgent.setServiceVersion(versionId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取服务版本信息异常", e);
        }

        serverVersion.setLinkx(linkx);
        serverVersion.setEAgent(eAgent);
        log.info("返回为：{}", serverVersion);
        return serverVersion;
    }
}
