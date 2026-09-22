package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserDeptNodeInfoVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImCommonService;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;

@Tag(name = "用户节点信息")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/users")
@RequiredArgsConstructor
@Validated
public class CollaborationUsersController {

    @Resource
    private ImCommonService imCommonService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;

    @Operation(summary = "查询当前服务器上是否存在此用户，不存在则返回空，存在则返回用户信息")
    @GetMapping("/{userId}")
    public R<ImUser> getByUserId(@Parameter(description = "用户ID") @PathVariable("userId") Long userId,
                             @Parameter(description = "目标节点 peerId，传入则查询对端节点数据") @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return nodeDispatchClient.dispatchAndParse(
                    peerId, "/collaboration/v1/users/" + userId, new HashMap<>(), R.class);
        }
        return R.success(imCommonService.getUserByUserId(String.valueOf(userId)));
    }

    @Operation(summary = "获取用户各节点信息")
    @GetMapping("/{userId}/profile")
    public R<ImUserDeptNodeInfoVO> userDeptNodeInfo(@PathVariable("userId") Long userId) {
        return R.success(imCommonService.userDeptNodeInfo(userId));
    }

    @Operation(summary = "查询当前服务器上的中心节点网关前缀")
    @GetMapping("/getPeerNodeGateWayPrefix")
    public R<String> getPeerNodeGateWayPrefix(@Parameter(description = "目标节点 peerId，传入则查询对端节点数据") @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return nodeDispatchClient.dispatchAndParse(
                    peerId, "/collaboration/v1/users/getPeerNodeGateWayPrefix", new HashMap<>(), R.class);
        }
        return R.success(imCommonService.getPeerNodeGateWayPrefix());
    }
}