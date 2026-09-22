package com.tdtech.linkx.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.linkx.node.dto.AuthorizedCallbackDTO;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.dto.PageReqDTO;
import com.tdtech.linkx.node.dto.P2pNodeRegisterDTO;
import com.tdtech.linkx.node.dto.PeerNodeClientUpdateDTO;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.GrantStatusEnum;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.enums.PeerNodeTypeEnum;
import com.tdtech.linkx.node.mapper.PeerNodeClientMapper;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.util.P2pHttpUtil;
import com.tdtech.linkx.node.util.PeerIdUtil;
import com.tdtech.linkx.node.vo.PeerNodeClientVO;
import com.tdtech.linkx.node.config.NodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeerNodeClientServiceImpl extends ServiceImpl<PeerNodeClientMapper, PeerNodeClient>
        implements IPeerNodeClientService {

    private final IPeerNodeStatusService peerNodeStatusService;
    private final IPeerNodeGrantService peerNodeGrantService;
    private final NodeProperties nodeProperties;

    @Override
    public PeerNodeClient getByPeerId(String peerId) {
        return getOne(new LambdaQueryWrapper<PeerNodeClient>()
                .eq(PeerNodeClient::getPeerId, peerId)
                .eq(PeerNodeClient::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createClient(String peerId, String ip, int port) {
        if (getByPeerId(peerId) != null) {
            log.info("Peer node client already exists: peerId={}", peerId);
            return;
        }

        PeerNodeClient client = new PeerNodeClient();
        client.setPeerId(peerId);
        client.setIp(ip);
        client.setPort(port);
        client.setGrant(0);
        client.setDeleted(0);
        client.setGmtCreated(LocalDateTime.now());
        save(client);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerClient(P2pNodeRegisterDTO dto) {
        String peerId = dto.getPeerId();
        PeerNodeClient existing = getByPeerId(peerId);
        if (existing != null) {
            updateExistingClient(existing, dto);
            return;
        }

        // 处理已删除节点重新注册：复活旧记录
        PeerNodeClient deletedExisting = getOne(new LambdaQueryWrapper<PeerNodeClient>()
                .eq(PeerNodeClient::getPeerId, peerId)
                .eq(PeerNodeClient::getDeleted, 1));
        if (deletedExisting != null) {
            log.info("registerClient: revive deleted record, peerId={}", peerId);
            reviveDeletedClient(deletedExisting, dto);
            return;
        }

        PeerNodeClient client = new PeerNodeClient();
        BeanUtils.copyProperties(dto, client);
        client.setPeerId(peerId);
        client.setCallbackUrl(dto.getCallback());
        client.setGrant(0);
        client.setDeleted(0);
        client.setGmtCreated(LocalDateTime.now());
        save(client);

        PeerNodeStatus status = new PeerNodeStatus();
        status.setPeerId(peerId);
        status.setPeerType(PeerNodeTypeEnum.SERVER.getCode());
        status.setStatus(NodeStatusEnum.DISCONNECTED.getCode());
        peerNodeStatusService.save(status);
        log.info("registerClient: created new client, peerId={}", peerId);
    }

    /**
     * 复活已删除的 client 记录。
     * 保留原 ID 和创建时间，重置授权状态和删除标记，更新节点信息。
     */
    private void reviveDeletedClient(PeerNodeClient existing, P2pNodeRegisterDTO dto) {
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getTag() != null) existing.setTag(dto.getTag());
        if (dto.getVersion() != null) existing.setVersion(dto.getVersion());
        if (dto.getCallback() != null) existing.setCallbackUrl(dto.getCallback());
        // 重置授权状态：复活后需要重新授权
        existing.setGrant(0);
        existing.setGrantDesc(null);
        existing.setGrantUserId(null);
        existing.setGrantUserName(null);
        existing.setGrantTime(null);
        existing.setExpiredIn(null);
        // 清除删除标记
        existing.setDeleted(0);
        existing.setDeletedTime(null);
        updateById(existing);

        // 重建状态记录（删除时已清理）
        PeerNodeStatus status = new PeerNodeStatus();
        status.setPeerId(existing.getPeerId());
        status.setPeerType(PeerNodeTypeEnum.SERVER.getCode());
        status.setStatus(NodeStatusEnum.DISCONNECTED.getCode());
        peerNodeStatusService.save(status);
        log.info("reviveDeletedClient: revived, peerId={}", existing.getPeerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClient(String peerId, PeerNodeClientUpdateDTO dto) {
        PeerNodeClient client = getByPeerId(peerId);
        if (client == null) {
            throw new BusinessException("客户端节点不存在: " + peerId);
        }

        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new BusinessException("未获取到登录用户信息");
        }

        // 先校验过期时间，避免已部分修改字段后才发现参数错误
        if (dto.getExpiredIn() != null) {
            LocalDateTime expiredTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(dto.getExpiredIn()), ZoneId.systemDefault());
            if (expiredTime.isBefore(LocalDateTime.now())) {
                throw new BusinessException("过期时间不能早于当前时间");
            }
            client.setExpiredIn(expiredTime);
        }

        Integer originalGrant = client.getGrant();
        boolean grantChanged = false;
        if (dto.getName() != null) {
            client.setName(dto.getName());
        }
        if (dto.getRemark() != null) {
            client.setRemark(dto.getRemark());
        }
        if (dto.getGrant() != null && !Objects.equals(originalGrant, dto.getGrant())) {
            client.setGrant(dto.getGrant());
            client.setGrantUserId(user.getUserId());
            client.setGrantUserName(user.getUserName());
            client.setGrantTime(LocalDateTime.now());
            grantChanged = true;
        }
        if (dto.getTag() != null) {
            client.setTag(dto.getTag());
        }

        updateById(client);

        // 事务外副作用：授权状态变更后通知对端，隔离异常避免影响事务提交
        if (grantChanged) {
            try {
                notifyAuthorizedCallback(client, dto.getGrant());
            } catch (Exception e) {
                log.error("updateClient: notifyAuthorizedCallback failed, peerId={}", peerId, e);
            }
        }
    }

    @Override
    public IPage<PeerNodeClientVO> pageClientsWithStatus(PageReqDTO pageReq) {
        IPage<PeerNodeClient> clientPage = page(
                new Page<>(pageReq.getPageNum(), pageReq.getPageSize()),
                new LambdaQueryWrapper<PeerNodeClient>().eq(PeerNodeClient::getDeleted, 0));
        Map<String, PeerNodeStatus> statusMap = loadStatusMap();
        Map<String, OpenDataGrantDTO> grantMap = loadGrantMap(clientPage.getRecords());

        List<PeerNodeClientVO> records = clientPage.getRecords().stream()
                .map(c -> buildClientVOWithStatus(c, statusMap.get(c.getPeerId()), grantMap.get(c.getPeerId())))
                .collect(Collectors.toList());

        Page<PeerNodeClientVO> resultPage = new Page<>(clientPage.getCurrent(), clientPage.getSize(), clientPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClient(Long id) {
        PeerNodeClient client = getById(id);
        if (client == null) {
            return;
        }
        logicalDelete(client);
        peerNodeStatusService.removeByPeerId(client.getPeerId());
        // 主动删除，通知对端取消授权
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        peerNodeGrantService.revokeGrant(localPeerId, client.getPeerId());

        // 事务外通知
        try {
            notifyRemoteDeleteServer(client);
        } catch (Exception e) {
            log.error("deleteClient: notifyRemoteDeleteServer failed, id={}", id, e);
        }
        log.info("Deleted peer node client (logical): id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClientByPeerId(String peerId) {
        PeerNodeClient client = getByPeerId(peerId);
        if (client == null) {
            log.info("Peer node client not found: peerId={}", peerId);
            return;
        }
        logicalDelete(client);
        peerNodeStatusService.removeByPeerId(peerId);
        // 被动接收对端删除通知，仅本地清理授权数据
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        peerNodeGrantService.revokeGrantFromRemote(localPeerId, peerId);
        log.info("Deleted peer node client by peerId (logical): peerId={}", peerId);
    }

    @Override
    public List<PeerNodeClient> getPeerNodeClientsByIp(String ip) {
        return list(new LambdaQueryWrapper<PeerNodeClient>()
                .eq(PeerNodeClient::getIp, ip)
                .eq(PeerNodeClient::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGrant(String peerId, Integer grant, String desc) {
        PeerNodeClient client = getByPeerId(peerId);
        if (client == null) {
            log.warn("updateGrant: peerId={} not found", peerId);
            return;
        }
        client.setGrant(grant);
        client.setGrantDesc(desc != null ? desc : "");
        client.setGrantTime(LocalDateTime.now());
        updateById(client);
        log.info("updateGrant: peerId={}, grant={}", peerId, grant);

        // 事务外通知
        try {
            notifyAuthorizedCallback(client, grant);
        } catch (Exception e) {
            log.error("updateGrant: notifyAuthorizedCallback failed, peerId={}", peerId, e);
        }
    }

    @Override
    public List<PeerNodeClientVO> listClientsByGrant(String grant) {
        List<PeerNodeClient> clients = list(new LambdaQueryWrapper<PeerNodeClient>()
                .eq(PeerNodeClient::getDeleted, 0));
        Map<String, PeerNodeStatus> statusMap = loadStatusMap();
        Map<String, OpenDataGrantDTO> grantMap = loadGrantMap(clients);
        return clients.stream()
                .filter(c -> filterByGrant(grantMap.get(c.getPeerId()), grant))
                .map(c -> buildClientVOWithStatus(c, statusMap.get(c.getPeerId()), grantMap.get(c.getPeerId())))
                .collect(Collectors.toList());
    }

    /**
     * 按数据授权维度过滤节点
     *
     * @param dto   节点的授权数据
     * @param grant 过滤条件：all=全部，org=组织部门，dashboard=看板，coopUser=协同用户
     */
    private boolean filterByGrant(OpenDataGrantDTO dto, String grant) {
        if (grant == null || "all".equalsIgnoreCase(grant)) {
            return true;
        }
        if (dto == null) {
            return false;
        }
        switch (grant.toLowerCase()) {
            case "org":
                return dto.getOrg() != null && dto.getOrg() == 1;
            case "dashboard":
                return dto.getDashboard() != null && dto.getDashboard() == 1;
            case "coopuser":
                return dto.getCoopUser() != null && dto.getCoopUser() == 1;
            case "h5":
                return dto.getH5() != null && dto.getH5() == 1;
            default:
                return true;
        }
    }

    private void updateExistingClient(PeerNodeClient existing, P2pNodeRegisterDTO dto) {
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getTag() != null) existing.setTag(dto.getTag());
        if (dto.getVersion() != null) existing.setVersion(dto.getVersion());
        if (dto.getCallback() != null) existing.setCallbackUrl(dto.getCallback());
        updateById(existing);
        log.info("registerClient: updated existing client, peerId={}", existing.getPeerId());
    }

    private void logicalDelete(PeerNodeClient client) {
        client.setDeleted(1);
        client.setDeletedTime(LocalDateTime.now());
        updateById(client);
    }

    private void notifyAuthorizedCallback(PeerNodeClient client, Integer grant) {
        String callbackUrl = client.getCallbackUrl();
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.warn("notifyAuthorizedCallback: callbackUrl is empty, peerId={}", client.getPeerId());
            return;
        }

        AuthorizedCallbackDTO callbackDTO = new AuthorizedCallbackDTO();
        callbackDTO.setAuthorized(grant);
        callbackDTO.setDesc(grant == 1 ? GrantStatusEnum.AUTHORIZED.getDesc() : (grant == 2 ? GrantStatusEnum.REJECTED.getDesc() : null));
        // 把本端授权给对端的有效期传给对端
        if (grant != null && grant == 1 && client.getExpiredIn() != null) {
            callbackDTO.setExpiredIn(client.getExpiredIn()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        P2pHttpUtil.postJson(callbackUrl, callbackDTO, "notifyAuthorizedCallback");
    }

    /**
     * 通知对端节点删除对应的 server 记录。
     * 本端删除 client（来自对端）→ 对端删除 server（指向本端，peerId=本端peerId）
     */
    private void notifyRemoteDeleteServer(PeerNodeClient client) {
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        String url = local.buildDeleteServerUrl(client.getIp(), localPeerId);
        P2pHttpUtil.delete(url, "notifyRemoteDeleteServer");
    }

    private Map<String, PeerNodeStatus> loadStatusMap() {
        return peerNodeStatusService.listByPeerType(PeerNodeTypeEnum.SERVER).stream()
                .collect(Collectors.toMap(PeerNodeStatus::getPeerId, s -> s, (a, b) -> a));
    }

    /**
     * 批量查询 client 授权给本端的维度。
     * client 表里的 peerId 是对端（注册方），grant 方向为 from=对端, to=本端（对端授权本端访问其数据）。
     */
    private Map<String, OpenDataGrantDTO> loadGrantMap(List<PeerNodeClient> clients) {
        if (clients == null || clients.isEmpty()) {
            return Collections.emptyMap();
        }
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        List<String> peerIds = clients.stream()
                .map(PeerNodeClient::getPeerId)
                .collect(Collectors.toList());
        return peerNodeGrantService.getGrantDTOMapByTo(localPeerId, peerIds);
    }

    private PeerNodeClientVO buildClientVOWithStatus(PeerNodeClient client, PeerNodeStatus status,
                                                      OpenDataGrantDTO grantDTO) {
        PeerNodeClientVO vo = new PeerNodeClientVO();
        BeanUtils.copyProperties(client, vo);
        vo.setGrantInfo();
        if (grantDTO != null) {
            vo.setOrg(grantDTO.getOrg());
            vo.setDashboard(grantDTO.getDashboard());
            vo.setCoopUser(grantDTO.getCoopUser());
            vo.setH5(grantDTO.getH5());
        }
        if (status != null) {
            vo.setStatusWithDesc(status.getStatus());
            vo.setSession(status.getSession());
            vo.setLastSeen(status.getLastSeen());
        } else {
            vo.setStatusDesc("从未连接");
        }
        return vo;
    }
}
