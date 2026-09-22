package com.tdtech.linkx.node.rpc;

import com.tdtech.linkx.node.api.PeerNodeRpcApi;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P2P 节点信息查询 RPC 实现
 */
@Slf4j
@DubboService
public class PeerNodeRpc implements PeerNodeRpcApi {

    @Resource
    private IPeerNodeServerService peerNodeServerService;
    @Resource
    private IPeerNodeClientService peerNodeClientService;

    @Override
    public Map<String, PeerNodeInfo> getPeerInfoMap(List<String> peerIds) {
        Map<String, PeerNodeInfo> result = new HashMap<>();
        if (peerIds == null || peerIds.isEmpty()) {
            return result;
        }
        for (String peerId : peerIds) {
            String name = null;
            String ip = null;
            PeerNodeServer server = peerNodeServerService.getByPeerId(peerId);
            if (server != null) {
                name = server.getName();
                ip = server.getIp();
            } else {
                PeerNodeClient client = peerNodeClientService.getByPeerId(peerId);
                if (client != null) {
                    name = client.getName();
                    ip = client.getIp();
                }
            }
            if (ip != null) {
                result.put(peerId, new PeerNodeInfo(name, ip));
            }
        }
        return result;
    }
}
