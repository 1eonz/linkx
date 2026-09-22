package com.tdtech.linkx.node.rpc;

import com.tdtech.linkx.node.api.P2PDataFlowRpcApi;
import com.tdtech.linkx.node.constants.Constants;
import com.tdtech.linkx.node.dto.DispatchResponseDTO;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.service.INodeDispatchService;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * P2P 数据流量 RPC 实现类
 */
@DubboService
public class P2PDataFlowRpc implements P2PDataFlowRpcApi {
    @Resource
    IPeerNodeServerService peerNodeServerService;

    @Resource
    IPeerNodeClientService peerNodeClientService;

    @Resource
    INodeDispatchService nodeDispatchService;

    @Resource
    IPeerNodeStatusService peerNodeStatusService;

    @Override
    public Map<String, Object> listNodes(String grant) {
        Map<String, Object> data = new HashMap<>(2);
//        data.put(Constants.LIST_NODES_KEY_CLIENTS, peerNodeClientService.listClientsByGrant(grant));
//        data.put(Constants.LIST_NODES_KEY_SERVERS, peerNodeServerService.listServersByGrant(grant));
        return data;
    }

    @Override
    public DispatchResponseDTO dispatch(String peerId, String originUri, String queryString) {
        return toDto(nodeDispatchService.dispatch(peerId, originUri, queryString));
    }

//    @Override
//    public DispatchResponseDTO proxy(String callerPeerId, String originUri, String queryString,
//                                     String authorizationHeader) {
//        return toDto(nodeDispatchService.proxy(callerPeerId, originUri, queryString,
//                authorizationHeader));
//    }

    @Override
    public Integer getPeerNodeStatus(String peerId) {
        PeerNodeStatus status = peerNodeStatusService.getByPeerId(peerId);
        return status == null ? NodeStatusEnum.DISCONNECTED.getCode() : status.getStatus();
    }

    /**
     * ResponseEntity 不可序列化，转换为可序列化的 DTO 供 Dubbo 跨进程传输
     */
    private DispatchResponseDTO toDto(ResponseEntity<byte[]> response) {
        if (response == null) {
            return null;
        }
        return new DispatchResponseDTO(response.getStatusCodeValue(), response.getBody());
    }
}