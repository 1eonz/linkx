package com.tdtech.linkx.node.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.entity.PeerNodeGrant;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.service.INodeDispatchService;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.vo.OpenDataStatisticVO;
import com.tdtech.linkx.node.vo.ReceivedGrantVO;
import com.tdtech.linkx.node.ws.client.P2pWebSocketClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/node/v1/p2p")
@RequiredArgsConstructor
@Tag(name = "开放数据管理", description = "P2P节点开放数据授权接口")
public class OpenDataController {

    private final IPeerNodeGrantService peerNodeGrantService;
    private final IPeerNodeServerService peerNodeServerService;
    private final IPeerNodeClientService peerNodeClientService;
    private final P2pWebSocketClient wsClient;
    private final INodeDispatchService nodeDispatchService;

    @Resource
    private ReportUtil reportUtil;

    @Operation(summary = "对服务器的开放数据授权", description = "管理后台更新对服务器的开放数据授权")
    @PostMapping("/servers/{peerId}/opendata/grant")
    public R<Void> grantServer(@PathVariable String peerId, @RequestBody @Valid OpenDataGrantDTO dto) {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_107.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_107.getMsg()));
        }
        PeerNodeServer server = peerNodeServerService.getByPeerId(peerId);
        String localPeerId = wsClient.getLocalPeerId();
        peerNodeGrantService.saveOrUpdateGrant(localPeerId, peerId, dto, user.getUserId());
        if (server != null) {
            String identifier = formatNodeIdentifier(server.getIp(), server.getPort());
            String detail = buildGrantDetail(identifier, dto);
            reportOperationLog(OperationTypeEnum.P2P_SERVER_GRANT_UPDATE, detail);
        }
        return R.success();
    }

    @Operation(summary = "查询对服务器的开放数据授权", description = "管理后台查询对服务器的授权信息")
    @GetMapping("/servers/{peerId}/opendata/grant")
    public R<OpenDataGrantDTO> getServerGrant(@PathVariable String peerId) {
        String localPeerId = wsClient.getLocalPeerId();
        OpenDataGrantDTO dto = peerNodeGrantService.getGrantDTO(localPeerId, peerId);
        return R.success(dto);
    }

    @Operation(summary = "对客户端的开放数据授权", description = "管理后台更新对客户端的开放数据授权")
    @PostMapping("/clients/{peerId}/opendata/grant")
    public R<Void> grantClient(@PathVariable String peerId, @RequestBody @Valid OpenDataGrantDTO dto) {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_107.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_107.getMsg()));
        }
        PeerNodeClient client = peerNodeClientService.getByPeerId(peerId);
        String localPeerId = wsClient.getLocalPeerId();
        peerNodeGrantService.saveOrUpdateGrant(localPeerId, peerId, dto, user.getUserId());
        if (client != null) {
            String identifier = formatNodeIdentifier(client.getIp(), client.getPort());
            String detail = buildGrantDetail(identifier, dto);
            reportOperationLog(OperationTypeEnum.P2P_CLIENT_GRANT_UPDATE, detail);
        }
        return R.success();
    }

    @Operation(summary = "查询对客户端的开放数据授权", description = "管理后台查询对客户端的授权信息")
    @GetMapping("/clients/{peerId}/opendata/grant")
    public R<OpenDataGrantDTO> getClientGrant(@PathVariable String peerId) {
        String localPeerId = wsClient.getLocalPeerId();
        OpenDataGrantDTO dto = peerNodeGrantService.getGrantDTO(localPeerId, peerId);
        return R.success(dto);
    }

    @Operation(summary = "查询对端节点开放数据统计",
            description = "聚合查询对端节点的组织部门（org）和协同岗（coopUser）统计数据")
    @GetMapping("/{peerId}/opendata/statistic")
    public R<OpenDataStatisticVO> statistic(@PathVariable String peerId) {
        OpenDataStatisticVO result = new OpenDataStatisticVO();

        try {
            ResponseEntity<byte[]> orgResp = nodeDispatchService.dispatch(
                    peerId, "/collaboration/v1/organization/tree", null);
            if (orgResp.getStatusCode().is2xxSuccessful() && orgResp.getBody() != null) {
                String json = new String(orgResp.getBody(), StandardCharsets.UTF_8);
                int orgCount = countOrgNodesFromR(json);
                OpenDataStatisticVO.OrgStatistic org = new OpenDataStatisticVO.OrgStatistic();
                org.setTotal(orgCount);
                result.setOrg(org);
            }
        } catch (Exception e) {
            log.warn("statistic: query org failed, peerId={}", peerId, e);
        }

        try {
            ResponseEntity<byte[]> coopResp = nodeDispatchService.dispatch(
                    peerId, "/collaboration/v1/statistics/count", null);
            if (coopResp.getStatusCode().is2xxSuccessful() && coopResp.getBody() != null) {
                String json = new String(coopResp.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> data = parseRData(json);
                if (data != null) {
                    OpenDataStatisticVO.CoopUserStatistic coopUser = new OpenDataStatisticVO.CoopUserStatistic();
                    coopUser.setTotal(toInt(data.get("total")));
                    coopUser.setUserTotal(toInt(data.get("userTotal")));
                    coopUser.setUserOnlineTotal(toInt(data.get("userOnlineTotal")));
                    result.setCoopUser(coopUser);
                }
            }
        } catch (Exception e) {
            log.warn("statistic: query coopUser failed, peerId={}", peerId, e);
        }

        return R.success(result);
    }

    /**
     * 解析 R 格式响应，提取 data 字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseRData(String json) {
        try {
            Map<String, Object> r = JsonUtil.parseJson(json, Map.class);
            if (r == null) {
                return null;
            }
            Object data = r.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
        } catch (Exception e) {
            log.warn("parseRData: failed, json={}", json, e);
        }
        return null;
    }

    /**
     * 从 R 格式响应中递归计算组织树节点数
     */
    @SuppressWarnings("unchecked")
    private int countOrgNodesFromR(String json) {
        try {
            Map<String, Object> r = JsonUtil.parseJson(json, Map.class);
            if (r == null) {
                return 0;
            }
            Object data = r.get("data");
            if (data instanceof Map) {
                return countOrgNodes((Map<String, Object>) data);
            }
        } catch (Exception e) {
            log.warn("countOrgNodesFromR: failed, json={}", json, e);
        }
        return 0;
    }

    /**
     * 递归计算组织树节点数（含子节点）
     */
    @SuppressWarnings("unchecked")
    private int countOrgNodes(Map<String, Object> node) {
        int count = 1;
        Object children = node.get("children");
        if (children instanceof List) {
            for (Object child : (List<Object>) children) {
                if (child instanceof Map) {
                    count += countOrgNodes((Map<String, Object>) child);
                }
            }
        }
        return count;
    }

    private int toInt(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatNodeIdentifier(String ip, Integer port) {
        return ip + ":" + port;
    }

    private void reportOperationLog(OperationTypeEnum type, String identifier) {
        OperationLog operationLog = new OperationLog(type);
        operationLog.setOperation(String.format(operationLog.getOperation(), identifier));
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
    }

    private String buildGrantDetail(String identifier, OpenDataGrantDTO dto) {
        String grants = Stream.of(
                dto.getOrg() != null && dto.getOrg() == 1 ? "组织部门数据" : null,
                dto.getDashboard() != null && dto.getDashboard() == 1 ? "看板数据" : null,
                dto.getCoopUser() != null && dto.getCoopUser() == 1 ? "协同用户数据" : null
        )
        .filter(Objects::nonNull)
        .collect(Collectors.joining("、"));

        return "授权项：" + identifier + ": " + (grants.isEmpty() ? "无" : grants);
    }

    private ReceivedGrantVO convertToReceivedGrantVO(PeerNodeGrant grant) {
        ReceivedGrantVO vo = new ReceivedGrantVO();
        vo.setId(grant.getId());
        vo.setFromPeerId(grant.getFromPeerId());
        vo.setFromPeerName(grant.getFromPeerName());
        vo.setToPeerId(grant.getToPeerId());
        vo.setGrantDescription(grant.getGrantDescription());
        vo.setExpiredTime(grant.getExpiredTime());
        vo.setGmtCreated(grant.getGmtCreated());

        if (grant.getGrantData() != null) {
            OpenDataGrantDTO dto = JsonUtil.parseJson(grant.getGrantData(), OpenDataGrantDTO.class);
            if (dto != null) {
                vo.setOrg(dto.getOrg());
                vo.setDashboard(dto.getDashboard());
                vo.setCoopUser(dto.getCoopUser());
            }
        }
        return vo;
    }
}
