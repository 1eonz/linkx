package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.AiRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.AiRecordQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationStatisticsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.OrganizationDiversionService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@DubboService
public class AiRpc implements AiRpcApi {

    /**
     * Agent服务端口，默认30280
     */
    @Value("${agent.service.port:30280}")
    private int agentServicePort;

    @Resource
    private CollaborationStatisticsService collaborationStatisticsService;
    @Resource
    private OrganizationDiversionService organizationDiversionService;
    @Resource
    private RestTemplate restTemplate;

    @Override
    public List<RecordCountResp> agentAskCount(AiRecordQO aiRecordQO, LinkedList<RecordCountReq.GroupEnum> groups) {
        List<String> collect = new ArrayList<>();
        if(StringUtils.isNotBlank(aiRecordQO.getDepartmentCode())){
            var imDepartments = organizationDiversionService.queryDepartmentForList(aiRecordQO.getDepartmentCode());
            if(CollectionUtils.isEmpty(imDepartments)){
                return Collections.emptyList();
            }
            collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        }
        return collaborationStatisticsService.countAgentRecords(collect, //
            DateFormatUtil.format(aiRecordQO.getStartTime(), DateFormatUtil.YYYY_MM_DD),//
            DateFormatUtil.format(aiRecordQO.getEndTime(), DateFormatUtil.YYYY_MM_DD),//
            aiRecordQO.getPersonName(),//
            null,//
            aiRecordQO.getCategory(),//
            groups);
    }

    @Override
    public Long getAgentCount() {
        try {
            String hostIp = getHostIp();
            String url = "http://" + hostIp + ":" + agentServicePort + "/agent/api/proxy/ai/v1/aiagent/management/count";
            log.info("Getting agent count from: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject jsonNode = JsonUtil.parseJson(response.getBody());
                if (jsonNode.containsKey("code") && jsonNode.getInteger("code") == 0) {
                    return jsonNode.getLong("data");
                }
                log.warn("Agent API returned error: {}", response.getBody());
            }
        } catch (Exception e) {
            log.warn("Failed to get agent count from agent service: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 获取宿主机IP
     * 优先从环境变量 SERVER_IP 获取（K8s注入），否则获取本机IP
     */
    private String getHostIp() {
        String serverIp = System.getenv("SERVER_IP");
        if (StringUtils.isNotBlank(serverIp)) {
            return serverIp;
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Failed to get local IP, using 127.0.0.1: {}", e.getMessage());
            return "127.0.0.1";
        }
    }

}
