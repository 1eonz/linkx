package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CoopUserShareDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserCandidateVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserShareVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.SharedNodeVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopSharedService;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 协同岗分享管理接口
 * 警信接口由分享方节点调用（所有节点共用同一警信服务器）。
 */
@Tag(name = "协同岗分享", description = "协同岗分享管理接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/coopusers")
@RequiredArgsConstructor
public class CoopUserShareController {

    private final IUserCoopSharedService coopSharedService;

    @GetMapping
    @Operation(summary = "查询协同岗列表（分页）", description = "按类型分页查询协同岗列表：0=全部；1=当前节点的；2=接收的，支持按名称搜索")
    public R<IPage<CoopUserShareVO>> listCoopUsers(
            @Parameter(description = "类型：0=全部；1=当前节点的；2=接收的")
            @RequestParam(defaultValue = "0") Integer type,
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小，默认为10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "协同岗名称（模糊搜索）")
            @RequestParam(required = false) String coopUserName) {

        return R.success(coopSharedService.pageCoopUsers(type, pageNum, pageSize, coopUserName));
    }

    @GetMapping("/group-candidates")
    @Operation(summary = "查询建群候选协同岗（分页）", description = "建群时按节点分页查询协同岗列表（含头像），支持按名称搜索。peerId 不传=本节点；传=对端分享给本节点的协同岗")
    public R<IPage<CoopUserCandidateVO>> listGroupCandidates(
            @Parameter(description = "目标节点 peerId，不传=本节点")
            @RequestParam(required = false) String peerId,
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小，默认为10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "协同岗名称（模糊搜索）")
            @RequestParam(required = false) String coopUserName) {

        return R.success(coopSharedService.pageGroupCandidates(peerId, pageNum, pageSize, coopUserName));
    }

    @PostMapping("/{coopUserId}/share")
    @Operation(summary = "分享协同岗", description = "将协同岗分享给指定节点。由分享方调用警信接口更新可见范围，再推WS通知接收方")
    public R<Void> shareCoopUser(
            @Parameter(description = "协同岗ID", required = true) @PathVariable Long coopUserId,
            @Valid @RequestBody CoopUserShareDTO dto) {

        UserInfo user = SecurityUtils.getUser();
        Long userId = (user != null) ? user.getUserId() : null;
        coopSharedService.shareCoopUser(coopUserId, dto.getPeerId(), dto.getOrgId(), dto.getOrgName(), userId);
        return R.success();
    }

    @PostMapping("/{coopUserId}/unshare")
    @Operation(summary = "取消分享协同岗", description = "取消协同岗分享。由取消方调用警信接口移除可见范围，再推WS通知接收方")
    public R<Void> unshareCoopUser(
            @Parameter(description = "协同岗ID", required = true) @PathVariable Long coopUserId,
            @Valid @RequestBody CoopUserShareDTO dto) {

        coopSharedService.unshareCoopUser(coopUserId, dto.getPeerId(), dto.getOrgId());
        return R.success();
    }

    @GetMapping("/{coopUserId}/shared-nodes")
    @Operation(summary = "查询协同岗已分享的节点列表", description = "根据协同岗ID查询该协同岗已分享给哪些节点")
    public R<List<SharedNodeVO>> listSharedNodes(
            @Parameter(description = "协同岗ID", required = true) @PathVariable Long coopUserId) {

        return R.success(coopSharedService.listSharedNodes(coopUserId));
    }
}
