package com.tdtech.linkx.node.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * P2P 节点信息查询 RPC 接口
 * 供其他微服务通过 Dubbo 调用，查询节点名称等信息
 */
public interface PeerNodeRpcApi {

    /**
     * 批量查询节点信息（名称+IP）
     *
     * @param peerIds 节点标识列表
     * @return peerId → 节点信息的映射，不存在的不包含在结果中
     */
    Map<String, PeerNodeInfo> getPeerInfoMap(List<String> peerIds);

    /**
     * 节点信息
     */
    class PeerNodeInfo implements Serializable {
        private String name;
        private String ip;

        public PeerNodeInfo() {}

        public PeerNodeInfo(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
    }
}

