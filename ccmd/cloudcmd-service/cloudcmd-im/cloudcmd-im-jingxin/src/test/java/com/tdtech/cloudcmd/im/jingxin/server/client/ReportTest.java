package com.tdtech.cloudcmd.im.jingxin.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.web.utils.HttpClientConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class ReportTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClientConfigurationProperties properties = new HttpClientConfigurationProperties();
        HttpClient client = new HttpClient(objectMapper, properties);
        URI uri = new URI("https://10.28.15.61:30844/msip/oam/v1/log");
        OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE);
        String s1 = client.putJson(uri, null, log, String.class);
    }
}
