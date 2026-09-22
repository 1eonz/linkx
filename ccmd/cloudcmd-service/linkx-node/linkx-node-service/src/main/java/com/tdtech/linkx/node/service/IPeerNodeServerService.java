package com.tdtech.linkx.node.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.linkx.node.dto.PageReqDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerCreateDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerUpdateDTO;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.vo.PeerNodeServerVO;

import java.util.List;

public interface IPeerNodeServerService extends IService<PeerNodeServer> {

    void createServer(PeerNodeServerCreateDTO dto);

    void updateServer(Long id, PeerNodeServerUpdateDTO dto);

    List<PeerNodeServer> listActiveServers();

    IPage<PeerNodeServerVO> pageServersWithStatus(PageReqDTO pageReq);

    PeerNodeServer getByPeerId(String peerId);

    void deleteServer(Long id);

    void deleteServerByPeerId(String peerId);

    List<PeerNodeServer> getPeerNodeServersByIp(String ip);

    void updateAuthorized(String peerId, Integer authorized, String desc, Long expiredIn);

    List<PeerNodeServerVO> listServersByGrant(String grant);
}
