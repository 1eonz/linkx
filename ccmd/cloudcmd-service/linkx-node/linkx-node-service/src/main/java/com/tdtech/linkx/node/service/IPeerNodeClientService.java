package com.tdtech.linkx.node.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.linkx.node.dto.PageReqDTO;
import com.tdtech.linkx.node.dto.P2pNodeRegisterDTO;
import com.tdtech.linkx.node.dto.PeerNodeClientUpdateDTO;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.vo.PeerNodeClientVO;

import java.util.List;

public interface IPeerNodeClientService extends IService<PeerNodeClient> {

    PeerNodeClient getByPeerId(String peerId);

    void createClient(String peerId, String ip, int port);

    void registerClient(P2pNodeRegisterDTO dto);

    void updateClient(String peerId, PeerNodeClientUpdateDTO dto);

    IPage<PeerNodeClientVO> pageClientsWithStatus(PageReqDTO pageReq);

    void deleteClient(Long id);

    void deleteClientByPeerId(String peerId);

    List<PeerNodeClient> getPeerNodeClientsByIp(String ip);

    void updateGrant(String peerId, Integer grant, String desc);

    List<PeerNodeClientVO> listClientsByGrant(String grant);
}
