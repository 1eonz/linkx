package com.tdtech.linkx.node.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.linkx.node.dto.AuthorizedCallbackDTO;
import com.tdtech.linkx.node.dto.PageReqDTO;
import com.tdtech.linkx.node.dto.P2pNodeRegisterDTO;
import com.tdtech.linkx.node.dto.PeerNodeClientUpdateDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerCreateDTO;
import com.tdtech.linkx.node.dto.PeerNodeServerUpdateDTO;
import com.tdtech.linkx.node.entity.PeerNodeClient;
import com.tdtech.linkx.node.entity.PeerNodeServer;
import com.tdtech.linkx.node.enums.GrantStatusEnum;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeServerService;
import com.tdtech.linkx.node.util.PeerIdUtil;
import com.tdtech.linkx.node.vo.PeerNodeClientVO;
import com.tdtech.linkx.node.vo.PeerNodeServerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/node/v1/p2p")
@RequiredArgsConstructor
@Tag(name = "P2P节点管理", description = "P2P节点创建、更新等接口")
public class P2pNodeController {

    private static final String REMOTE_IP_HEADER = "X-CloudCmd-RemoteIp";

    private final IPeerNodeServerService peerNodeServerService;
    private final IPeerNodeClientService peerNodeClientService;

    @Resource
    private ReportUtil reportUtil;

    @Operation(summary = "创建服务器节点", description = "用于本节点连接该服务器节点")
    @PostMapping("/servers")
    public R<Void> createServer(@RequestBody @Valid PeerNodeServerCreateDTO dto) {
        peerNodeServerService.createServer(dto);
        reportOperationLog(OperationTypeEnum.P2P_SERVER_INSERT, buildCreateDetail(dto));
        return R.success();
    }

    @Operation(summary = "更新服务器节点", description = "更新服务器节点的IP、端口、名称等信息")
    @PutMapping("/servers/{id}")
    public R<Void> updateServer(@PathVariable Long id, @RequestBody @Valid PeerNodeServerUpdateDTO dto) {
        PeerNodeServer oldServer = peerNodeServerService.getById(id);
        peerNodeServerService.updateServer(id, dto);
        if (oldServer != null) {
            reportOperationLog(OperationTypeEnum.P2P_SERVER_UPDATE, buildServerChangedFields(oldServer, dto));
        }
        return R.success();
    }

    @Operation(summary = "查询服务器节点列表", description = "查询未被删除的服务器节点列表（包含连接状态）")
    @GetMapping("/servers")
    public R<PageResult<PeerNodeServerVO>> listServers(@Valid PageReqDTO pageReq) {
        return R.success(buildPageResult(peerNodeServerService.pageServersWithStatus(pageReq)));
    }

    @Operation(summary = "删除服务器节点", description = "逻辑删除指定服务器节点")
    @DeleteMapping("/servers/{id}")
    public R<Void> deleteServer(@PathVariable Long id) {
        PeerNodeServer server = peerNodeServerService.getById(id);
        peerNodeServerService.deleteServer(id);
        if (server != null) {
            reportOperationLog(OperationTypeEnum.P2P_SERVER_DELETE, server.getIp() + ":" + server.getPort());
        }
        return R.success();
    }

    @Operation(summary = "查询客户端节点列表", description = "查询客户端节点列表")
    @GetMapping("/clients")
    public R<PageResult<PeerNodeClientVO>> listClients(@Valid PageReqDTO pageReq) {
        return R.success(buildPageResult(peerNodeClientService.pageClientsWithStatus(pageReq)));
    }

    @Operation(summary = "更新P2P客户端节点信息", description = "更新P2P客户端节点信息（授权等）")
    @PostMapping("/clients/{peerId}")
    public R<Void> updateClient(@PathVariable String peerId, @RequestBody @Valid PeerNodeClientUpdateDTO dto) {
        PeerNodeClient client = peerNodeClientService.getByPeerId(peerId);
        peerNodeClientService.updateClient(peerId, dto);
        if (client != null) {
            reportOperationLog(OperationTypeEnum.P2P_CLIENT_UPDATE, buildClientChangedFields(client, dto));
        }
        return R.success();
    }

    @Operation(summary = "删除客户端节点", description = "删除指定的客户端节点")
    @DeleteMapping("/clients/{id}")
    public R<Void> deleteClient(@PathVariable Long id) {
        PeerNodeClient client = peerNodeClientService.getById(id);
        peerNodeClientService.deleteClient(id);
        if (client != null) {
            reportOperationLog(OperationTypeEnum.P2P_CLIENT_DELETE, client.getIp() + ":" + client.getPort());
        }
        return R.success();
    }

    @Operation(summary = "删除客户端节点通知", description = "对端节点删除服务器节点时通知本节点删除对应客户端记录（IP白名单校验）")
    @DeleteMapping("/clients/delete/{peerId}")
    public R<Void> deleteClientByPeerId(@PathVariable String peerId, HttpServletRequest request) {
        String remoteIp = getRemoteIp(request);
        String failReason = checkIpWhitelist(remoteIp, null, peerId);
        if (failReason != null) {
            log.warn("删除客户端节点通知: IP白名单校验失败, peerId={}, {}", peerId, failReason);
            return R.failure(403, "IP校验失败");
        }
        peerNodeClientService.deleteClientByPeerId(peerId);
        log.info("删除客户端节点通知: peerId={}", peerId);
        return R.success();
    }

    @Operation(summary = "删除服务器节点通知", description = "对端节点删除客户端节点时通知本节点删除对应服务器记录（IP白名单校验）")
    @DeleteMapping("/servers/delete/{peerId}")
    public R<Void> deleteServerByPeerId(@PathVariable String peerId, HttpServletRequest request) {
        String remoteIp = getRemoteIp(request);
        String failReason = checkIpWhitelist(remoteIp, null, peerId);
        if (failReason != null) {
            log.warn("删除服务器节点通知: IP白名单校验失败, peerId={}, {}", peerId, failReason);
            return R.failure(403, "IP校验失败");
        }
        peerNodeServerService.deleteServerByPeerId(peerId);
        log.info("删除服务器节点通知: peerId={}", peerId);
        return R.success();
    }

    @Operation(summary = "接收对端节点注册通知", description = "对端节点主动连接时通知本节点创建客户端记录（IP白名单校验）")
    @PostMapping("/register")
    public R<Void> registerNode(@RequestBody @Valid P2pNodeRegisterDTO dto, HttpServletRequest request) {
        String remoteIp = getRemoteIp(request);
        // 校验 remoteIp / dto.ip / dto.peerId 解析 IP 三者一致
        String failReason = checkIpWhitelist(remoteIp, dto.getIp(), dto.getPeerId());
        if (failReason != null) {
            log.warn("节点注册: IP白名单校验失败, {}", failReason);
            return R.failure(403, "IP校验失败");
        }
        peerNodeClientService.registerClient(dto);
        log.info("节点注册: peerId={}, ip={}, port={}, name={}, callback={}", dto.getPeerId(), dto.getIp(), dto.getPort(), dto.getName(), dto.getCallback());
        return R.success();
    }

    @Operation(summary = "节点数据互通授权回调", description = "对端节点通知本节点授权结果（IP白名单校验）")
    @PostMapping("/{peerId}/authorized/callback")
    public R<Void> authorizedCallback(
            @PathVariable String peerId,
            @RequestBody @Valid AuthorizedCallbackDTO dto,
            HttpServletRequest request) {
        // peerId 是 server.peerId（对端 peerId），对端用这个 peerId 作为回调标识
        // 本端通过 peerId 直接查 server 表
        PeerNodeServer server = peerNodeServerService.getByPeerId(peerId);
        if (server == null) {
            log.warn("授权回调: peerId={} 不存在", peerId);
            return R.failure(404, "节点不存在");
        }
        peerNodeServerService.updateAuthorized(peerId, dto.getAuthorized(), dto.getDesc(), dto.getExpiredIn());
        log.info("授权回调: peerId={}, authorized={}, desc={}, expiredIn={}",
                peerId, dto.getAuthorized(), dto.getDesc(), dto.getExpiredIn());
        return R.success();
    }

    @Operation(summary = "拒绝客户端节点", description = "拒绝指定客户端节点的数据互通请求")
    @PutMapping("/clients/{peerId}/reject")
    public R<Void> rejectClient(@PathVariable String peerId, @RequestParam(required = false) String desc) {
        PeerNodeClient client = peerNodeClientService.getByPeerId(peerId);
        if (client == null) {
            return R.failure(404, "客户端节点不存在");
        }
        if (StringUtils.isEmpty(desc)) desc = GrantStatusEnum.REJECTED.getDesc();
        peerNodeClientService.updateGrant(peerId, GrantStatusEnum.REJECTED.getCode(), desc);
        reportOperationLog(OperationTypeEnum.P2P_CLIENT_UPDATE, peerId, "拒绝授权" + (desc != null ? ": " + desc : ""));
        return R.success();
    }

    @Operation(summary = "获取节点信息列表", description = "供当前服务器客户端获取和本局点有关联的所有P2P节点列表")
    @GetMapping
    public R<Map<String, Object>> listNodes(@RequestParam(required = false, defaultValue = "all") String grant) {
        Map<String, Object> data = new HashMap<>(2);
        data.put("clients", peerNodeClientService.listClientsByGrant(grant));
        data.put("servers", peerNodeServerService.listServersByGrant(grant));
        return R.success(data);
    }

    private String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader(REMOTE_IP_HEADER);
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String checkIpWhitelist(String remoteIp, String expectedIp, String peerId) {
        if (peerId != null && !peerId.isEmpty()) {
            try {
                PeerIdUtil.getIp(peerId);
            } catch (Exception e) {
                return String.format("invalid peerId=%s", peerId);
            }
        }
        return null;
    }

    private <T> PageResult<T> buildPageResult(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
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

    private void reportOperationLog(OperationTypeEnum type, String identifier, String detail) {
        OperationLog operationLog = new OperationLog(type);
        operationLog.setOperation(String.format(operationLog.getOperation(), identifier) + " " + detail);
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
    }

    private String buildCreateDetail(PeerNodeServerCreateDTO dto) {
        StringBuilder sb = new StringBuilder("[ip=").append(dto.getIp());
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            sb.append(", 节点名称=").append(dto.getName());
        }
        if (dto.getTag() != null && !dto.getTag().isEmpty()) {
            sb.append(", 标签=").append(dto.getTag());
        }
        return sb.append("]").toString();
    }

    private String buildServerChangedFields(PeerNodeServer old, PeerNodeServerUpdateDTO dto) {
        StringBuilder sb = new StringBuilder("[");
        boolean hasChange = appendChange(sb, false, "ip", old.getIp(), dto.getIp());
        hasChange = appendChange(sb, hasChange, "节点名称", old.getName(), dto.getName());
        hasChange = appendChange(sb, hasChange, "标签", old.getTag(), dto.getTag());
        return hasChange ? sb.append("]").toString() : "";
    }

    private String buildClientChangedFields(PeerNodeClient old, PeerNodeClientUpdateDTO dto) {
        StringBuilder sb = new StringBuilder("[");
        boolean hasChange = appendChange(sb, false, "节点名称", old.getName(), dto.getName());
        hasChange = appendChange(sb, hasChange, "备注", old.getRemark(), dto.getRemark());
        hasChange = appendChange(sb, hasChange, "授权", formatGrant(old.getGrant()), formatGrant(dto.getGrant()));
        if (dto.getExpiredIn() != null) {
            LocalDateTime newTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dto.getExpiredIn()), ZoneId.systemDefault());
            hasChange = appendChange(sb, hasChange, "过期时间", old.getExpiredIn(), newTime);
        }
        hasChange = appendChange(sb, hasChange, "标签", old.getTag(), dto.getTag());
        return hasChange ? sb.append("]").toString() : "";
    }

    private boolean appendChange(StringBuilder sb, boolean hasChange, String label, Object oldVal, Object newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            if (hasChange) sb.append(", ");
            sb.append(label).append(": ").append(oldVal).append("→").append(newVal);
            return true;
        }
        return hasChange;
    }

    private String formatGrant(Integer grant) {
        if (grant == null) return "";
        switch (grant) {
            case 1: return "已授权";
            case 2: return "拒绝";
            default: return "未授权";
        }
    }
}
