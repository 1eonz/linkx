package com.tdtech.linkx.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.entity.PeerNodeGrant;

import java.util.List;
import java.util.Map;

public interface IPeerNodeGrantService extends IService<PeerNodeGrant> {

    List<PeerNodeGrant> listByFromPeerId(String fromPeerId);

    List<PeerNodeGrant> listByToPeerId(String toPeerId);

    PeerNodeGrant getByFromAndTo(String fromPeerId, String toPeerId);

    void saveGrant(String fromPeerId, String fromPeerName, String toPeerId, String toPeerName,
                    String permission, String grantData, Long grantUserId);

    void revokeGrant(String fromPeerId, String toPeerId);

    /**
     * 取消授权（被动接收对端通知时使用，不会再次通知对端）
     */
    void revokeGrantFromRemote(String fromPeerId, String toPeerId);

    void saveOrUpdateGrant(String fromPeerId, String toPeerId, OpenDataGrantDTO dto, Long userId);

    /**
     * 保存或更新授权数据（被动接收对端通知时使用，不会再次通知对端）
     */
    void saveOrUpdateGrantFromRemote(String fromPeerId, String toPeerId, OpenDataGrantDTO dto);

    OpenDataGrantDTO getGrantDTO(String fromPeerId, String toPeerId);

    Map<String, OpenDataGrantDTO> getGrantDTOMap(String fromPeerId, List<String> toPeerIds);

    /**
     * 按 toPeerId + fromPeerIds 批量查询授权数据，返回 Map<fromPeerId, DTO>。
     * 用于"本端作为被授权方"的场景（如查询哪些 client 授权给了本端）。
     */
    Map<String, OpenDataGrantDTO> getGrantDTOMapByTo(String toPeerId, List<String> fromPeerIds);

}
