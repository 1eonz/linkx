package com.tdtech.linkx.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.PeerNodeTypeEnum;

import java.util.List;

public interface IPeerNodeStatusService extends IService<PeerNodeStatus> {

    PeerNodeStatus getByPeerId(String peerId);

    List<PeerNodeStatus> listByPeerType(PeerNodeTypeEnum peerType);

    List<PeerNodeStatus> listAll();

    void updateStatus(String peerId, Integer status);

    void updateSession(String peerId, String session);

    void updateLastSeen(String peerId);

    void updateAuthFail(String peerId, String reason);

    void updateRetryStrategy(String peerId, Integer retryStrategy);

    void updateJwt(String peerId, String jwt);

    void removeByPeerId(String peerId);
}
