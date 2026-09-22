package com.tdtech.linkx.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.dto.PageReqDTO;
import com.tdtech.linkx.node.dto.P2pNodeRegisterDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerCreateDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerUpdateDTO;
import com.tdtech.linkx.node.dto.OpenDataGrantDTO;
import com.tdtech.linkx.node.util.VersionUtil;
import org.springframework.beans.BeanUtils;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.entity.PeerNodeStatus;
import com.tdtech.linkx.node.enums.NodeStatusEnum;
import com.tdtech.linkx.node.enums.PeerNodeTypeEnum;
import com.tdtech.linkx.node.mapper.PeerNodeServerMapper;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeGrantService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.util.P2pHttpUtil;
import com.tdtech.linkx.node.util.IpUtil;
import com.tdtech.linkx.node.util.PeerIdUtil;
import com.tdtech.linkx.node.vo.PeerNodeServerVO;
import com.tdtech.linkx.node.ws.client.P2pWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeerNodeServerServiceImpl extends ServiceImpl<PeerNodeServerMapper, PeerNodeServer>
        implements IPeerNodeServerService {

    private final IPeerNodeStatusService peerNodeStatusService;
    private final IPeerNodeClientService peerNodeClientService;
    private final IPeerNodeGrantService peerNodeGrantService;
    private final P2pWebSocketClient wsClient;
    private final NodeProperties nodeProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createServer(PeerNodeServerCreateDTO dto) {
        validateServerIp(dto.getIp());

        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String peerId = PeerIdUtil.generatePeerId(dto.getIp(), local.getPort());
        // 本端 peerId，用于通知对端注册
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new BusinessException("未获取到登录用户信息");
        }

        checkIpDuplicate(dto.getIp());

        // 处理已删除节点重新创建：复活旧记录
        PeerNodeServer deletedExisting = getOne(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getIp, dto.getIp())
                .eq(PeerNodeServer::getDeleted, 1));
        if (deletedExisting != null) {
            log.info("createServer: revive deleted record, ip={}", dto.getIp());
            reviveDeletedServer(deletedExisting, dto, peerId, localPeerId, user, local);
            return;
        }

        PeerNodeServer server = new PeerNodeServer();
        BeanUtils.copyProperties(dto, server);
        server.setPeerId(peerId);
        // port 由本端统一配置决定（所有节点共用同一端口），DTO 已不传 port
        server.setPort(local.getPort());
        server.setVersion(VersionUtil.getProjectVersion());
        // callbackUrl 中携带 server.peerId（对端 peerId），对端回调时本端按此 peerId 查 server 表
        server.setCallbackUrl(local.buildCallbackUrl(peerId));
        server.setCreateUserId(user.getUserId());
        server.setCreateUserName(user.getUserName());
        server.setDeleted(0);
        server.setAuthorized(0);
        server.setGmtCreated(LocalDateTime.now());
        save(server);

        saveInitialStatus(peerId, PeerNodeTypeEnum.CLIENT);

        // 事务外通知：隔离异常，避免外部调用失败拖垮 DB 事务
        try {
            notifyRemoteRegister(dto, peerId, localPeerId);
        } catch (Exception e) {
            log.error("createServer: notifyRemoteRegister failed, peerId={}", peerId, e);
        }
    }

    /**
     * 复活已删除的 server 记录。
     * 保留原 ID 和创建时间，重置授权状态和删除标记，更新节点信息。
     */
    private void reviveDeletedServer(PeerNodeServer existing, PeerNodeServerCreateDTO dto,
                                      String peerId, String localPeerId,
                                      UserInfo user, NodeProperties.LocalNode local) {
        BeanUtils.copyProperties(dto, existing);
        existing.setPeerId(peerId);
        // port 由本端统一配置决定（所有节点共用同一端口），DTO 已不传 port
        existing.setPort(local.getPort());
        existing.setVersion(VersionUtil.getProjectVersion());
        existing.setCallbackUrl(local.buildCallbackUrl(peerId));
        existing.setCreateUserId(user.getUserId());
        existing.setCreateUserName(user.getUserName());
        // 重置授权状态：复活后需要重新走授权流程
        existing.setAuthorized(0);
        existing.setAuthorizedDesc("");
        existing.setAuthorizedTime(null);
        existing.setExpiredIn(null);
        // 清除删除标记
        existing.setDeleted(0);
        existing.setDeletedTime(null);
        updateById(existing);

        // 重建状态记录（删除时已清理）
        saveInitialStatus(peerId, PeerNodeTypeEnum.CLIENT);

        // 事务外通知
        try {
            notifyRemoteRegister(dto, peerId, localPeerId);
        } catch (Exception e) {
            log.error("reviveDeletedServer: notifyRemoteRegister failed, peerId={}", peerId, e);
        }
        log.info("reviveDeletedServer: revived, ip={}, peerId={}", dto.getIp(), peerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServer(Long id, PeerNodeServerUpdateDTO dto) {
        PeerNodeServer server = getById(id);
        if (server == null) {
            throw new BusinessException("服务器节点不存在：" + id);
        }
        if (!IpUtil.isValidIpv4(dto.getIp())) {
            throw new BusinessException("IP格式异常：" + dto.getIp());
        }

        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String newPeerId = PeerIdUtil.generatePeerId(dto.getIp(), local.getPort());
        if (!StringUtils.isEquals(dto.getIp(), server.getIp())) {
            if (getByPeerId(newPeerId) != null) {
                throw new BusinessException("服务器节点已存在：" + dto.getIp());
            }
            peerNodeStatusService.removeByPeerId(server.getPeerId());
            saveInitialStatus(newPeerId, PeerNodeTypeEnum.CLIENT);
        }

        server.setPeerId(newPeerId);
        BeanUtils.copyProperties(dto, server);
        // port 由本端统一配置决定（所有节点共用同一端口），DTO 已不传 port
        server.setPort(local.getPort());
        updateById(server);
    }

    @Override
    public List<PeerNodeServer> listActiveServers() {
        return list(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getDeleted, 0)
                .eq(PeerNodeServer::getAuthorized, 1));
    }

    @Override
    public IPage<PeerNodeServerVO> pageServersWithStatus(PageReqDTO pageReq) {
        IPage<PeerNodeServer> serverPage = page(
                new Page<>(pageReq.getPageNum(), pageReq.getPageSize()),
                new LambdaQueryWrapper<PeerNodeServer>().eq(PeerNodeServer::getDeleted, 0));

        Map<String, PeerNodeStatus> statusMap = loadStatusMap(PeerNodeTypeEnum.CLIENT);
        Map<String, OpenDataGrantDTO> grantMap = loadGrantMap(serverPage.getRecords());
        List<PeerNodeServerVO> records = serverPage.getRecords().stream()
                .map(s -> buildServerVOWithStatus(s, statusMap.get(s.getPeerId()), grantMap.get(s.getPeerId())))
                .collect(Collectors.toList());

        Page<PeerNodeServerVO> resultPage = new Page<>(serverPage.getCurrent(), serverPage.getSize(), serverPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public PeerNodeServer getByPeerId(String peerId) {
        return getOne(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getPeerId, peerId)
                .eq(PeerNodeServer::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long id) {
        PeerNodeServer server = getById(id);
        if (server == null) {
            return;
        }

        server.setDeleted(1);
        server.setDeletedTime(LocalDateTime.now());
        updateById(server);

        peerNodeStatusService.removeByPeerId(server.getPeerId());
        String localPeerId = wsClient.getLocalPeerId();
        if (localPeerId != null) {
            peerNodeGrantService.revokeGrant(server.getPeerId(), localPeerId);
        }

        // 事务外副作用：隔离异常，避免影响事务提交
        try {
            wsClient.disconnect(server.getPeerId());
        } catch (Exception e) {
            log.error("deleteServer: wsClient.disconnect failed, peerId={}", server.getPeerId(), e);
        }
        try {
            notifyRemoteDeleteClient(server);
        } catch (Exception e) {
            log.error("deleteServer: notifyRemoteDeleteClient failed, peerId={}", server.getPeerId(), e);
        }
        log.info("Deleted peer node server (logical): id={}, peerId={}", id, server.getPeerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServerByPeerId(String peerId) {
        PeerNodeServer server = getByPeerId(peerId);
        if (server == null) {
            log.info("Peer node server not found: peerId={}", peerId);
            return;
        }
        server.setDeleted(1);
        server.setDeletedTime(LocalDateTime.now());
        updateById(server);

        peerNodeStatusService.removeByPeerId(peerId);
        String localPeerId = wsClient.getLocalPeerId();
        if (localPeerId != null) {
            peerNodeGrantService.revokeGrantFromRemote(peerId, localPeerId);
        }

        try {
            wsClient.disconnect(peerId);
        } catch (Exception e) {
            log.error("deleteServerByPeerId: wsClient.disconnect failed, peerId={}", peerId, e);
        }
        log.info("Deleted peer node server by peerId (logical): peerId={}", peerId);
    }

    @Override
    public List<PeerNodeServer> getPeerNodeServersByIp(String ip) {
        return list(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getIp, ip)
                .eq(PeerNodeServer::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuthorized(String peerId, Integer authorized, String desc, Long expiredIn) {
        PeerNodeServer server = getOne(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getPeerId, peerId));
        if (server == null) {
            log.warn("updateAuthorized: peerId={} not found", peerId);
            return;
        }
        server.setAuthorized(authorized);
        server.setAuthorizedDesc(desc != null ? desc : "");
        server.setAuthorizedTime(LocalDateTime.now());
        if (expiredIn != null) {
            server.setExpiredIn(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(expiredIn), ZoneId.systemDefault()));
        }
        updateById(server);

        // 更新连接状态：授权通过→连接中（触发建链），其他→已断开
        NodeStatusEnum newStatus = (authorized != null && authorized == 1)
                ? NodeStatusEnum.CONNECTING : NodeStatusEnum.DISCONNECTED;
        peerNodeStatusService.updateStatus(peerId, newStatus.getCode());

        log.info("updateAuthorized: peerId={}, authorized={}, expiredIn={}, status={}",
                peerId, authorized, server.getExpiredIn(), newStatus);
    }

    @Override
    public List<PeerNodeServerVO> listServersByGrant(String grant) {
        List<PeerNodeServer> servers = list(new LambdaQueryWrapper<PeerNodeServer>()
                .eq(PeerNodeServer::getDeleted, 0));
        Map<String, PeerNodeStatus> statusMap = loadStatusMap(PeerNodeTypeEnum.CLIENT);
        Map<String, OpenDataGrantDTO> grantMap = loadGrantMap(servers);
        return servers.stream()
                .filter(s -> filterByGrant(grantMap.get(s.getPeerId()), grant))
                .map(s -> buildServerVOWithStatus(s, statusMap.get(s.getPeerId()), grantMap.get(s.getPeerId())))
                .collect(Collectors.toList());
    }

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
            default:
                return true;
        }
    }

    private void validateServerIp(String ip) {
        if (!IpUtil.isValidIpv4(ip)) {
            throw new BusinessException("IP格式异常：" + ip);
        }
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        if (ip.equals(local.getEffectiveIp())) {
            throw new BusinessException("不能创建指向本机的服务端配置：" + ip);
        }
        if (ip.startsWith("127.0.")) {
            throw new BusinessException("不能创建回环地址的服务端配置：" + ip);
        }
    }

    private void checkIpDuplicate(String ip) {
        if (!getPeerNodeServersByIp(ip).isEmpty()) {
            throw new BusinessException("已存在相同的服务器节点：" + ip);
        }
        if (!peerNodeClientService.getPeerNodeClientsByIp(ip).isEmpty()) {
            throw new BusinessException("已存在相同的客户端节点：" + ip);
        }
    }

    private void saveInitialStatus(String peerId, PeerNodeTypeEnum peerType) {
        // 先清理可能残留的旧 status 记录（避免唯一索引冲突）
        peerNodeStatusService.removeByPeerId(peerId);
        PeerNodeStatus status = new PeerNodeStatus();
        status.setPeerId(peerId);
        status.setPeerType(peerType.getCode());
        status.setStatus(NodeStatusEnum.DISCONNECTED.getCode());
        peerNodeStatusService.save(status);
    }

    /**
     * 通知对端节点注册 client 记录。
     *
     * @param dto          创建 server 的请求参数（含对端 IP）
     * @param serverPeerId 本端 server.peerId（基于对端 IP 生成），用于构造 callbackUrl，
     *                     对端回调时本端按此 peerId 查 server 表
     * @param localPeerId  本端 peerId（基于本端 IP 生成），作为对端 client.peerId
     */
    private void notifyRemoteRegister(PeerNodeServerCreateDTO dto, String serverPeerId, String localPeerId) {
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        P2pNodeRegisterDTO registerDTO = new P2pNodeRegisterDTO();
        BeanUtils.copyProperties(local, registerDTO);
        registerDTO.setPeerId(localPeerId);
        registerDTO.setIp(local.getEffectiveIp());
        registerDTO.setVersion(VersionUtil.getProjectVersion());
        registerDTO.setCallback(local.buildCallbackUrl(serverPeerId));

        String url = local.buildRegisterUrl(dto.getIp());
        P2pHttpUtil.postJson(url, registerDTO, "notifyRemoteRegister");
    }

    /**
     * 通知对端节点删除对应的 client 记录。
     * 本端删除 server（指向对端）→ 对端删除 client（来自本端，peerId=本端peerId）
     */
    private void notifyRemoteDeleteClient(PeerNodeServer server) {
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        String url = local.buildDeleteClientUrl(server.getIp(), localPeerId);
        P2pHttpUtil.delete(url, "notifyRemoteDeleteClient");
    }

    private Map<String, PeerNodeStatus> loadStatusMap(PeerNodeTypeEnum peerType) {
        return peerNodeStatusService.listByPeerType(peerType).stream()
                .collect(Collectors.toMap(PeerNodeStatus::getPeerId, s -> s, (a, b) -> a));
    }

    /**
     * 批量查询本端对 server 的授权维度。
     * grant 表中 from=本端peerId, to=server.peerId
     */
    private Map<String, OpenDataGrantDTO> loadGrantMap(List<PeerNodeServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return Collections.emptyMap();
        }
        NodeProperties.LocalNode local = nodeProperties.getLocal();
        String localPeerId = PeerIdUtil.generatePeerId(local.getEffectiveIp(), local.getPort());
        List<String> peerIds = servers.stream()
                .map(PeerNodeServer::getPeerId)
                .collect(Collectors.toList());
        return peerNodeGrantService.getGrantDTOMap(localPeerId, peerIds);
    }

    private PeerNodeServerVO buildServerVOWithStatus(PeerNodeServer server, PeerNodeStatus status,
                                                      OpenDataGrantDTO grantDTO) {
        PeerNodeServerVO vo = new PeerNodeServerVO();
        BeanUtils.copyProperties(server, vo);
        if (grantDTO != null) {
            vo.setOrg(grantDTO.getOrg());
            vo.setDashboard(grantDTO.getDashboard());
            vo.setCoopUser(grantDTO.getCoopUser());
            vo.setH5(grantDTO.getH5());
        }
        if (status != null) {
            vo.setStatusWithDesc(status.getStatus());
            vo.setRetryStrategy(status.getRetryStrategy());
            vo.setLastAuthFailReason(status.getLastAuthFailReason());
            vo.setSession(status.getSession());
            vo.setLastSeen(status.getLastSeen());
        }
        return vo;
    }
}
