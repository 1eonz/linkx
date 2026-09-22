package com.tdtech.cloudcmd.linkx.third.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.linkx.third.service.LocationShareService;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareCreateVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareExitVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareJoinVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "位置共享", description = "位置共享相关接口")
@RestController
@RequestMapping("/third/v1/locationshare")
@RequiredArgsConstructor
public class LocationShareController {

    private final LocationShareService locationShareService;

    @PostMapping
    @Operation(summary = "创建位置共享", description = "创建位置共享Action，同时将创建人作为成员加入，并支持分发到群组或用户")
    public R<Long> createLocationShare(
        @Parameter(description = "位置共享信息", required = true) @Valid @RequestBody LocationShareCreateVo vo) {
        Long shareId = locationShareService.createLocationShare(vo);
        return R.success(shareId);
    }

    @GetMapping("/my")
    @Operation(summary = "获取位置共享", description = "获取当前用户正在进行的位置共享详情，包含成员列表")
    public R<LocationShareDetailVo> getMy(
        @Parameter(description = "位置共享会话ID", required = true) @RequestParam Long shareId) {
        return R.success(locationShareService.getMyLocationShare(shareId));
    }

    @PostMapping("/{shareId}/join")
    @Operation(summary = "加入位置共享", description = "用户加入指定的位置共享")
    public R<Void> join(
        @Parameter(description = "位置共享ID", in = ParameterIn.PATH, required = true) @PathVariable Long shareId,
        @Parameter(description = "加入信息", required = true) @Valid @RequestBody LocationShareJoinVo vo) {
        locationShareService.joinLocationShare(shareId, vo);
        return R.success();
    }

    @PostMapping("/{shareId}/exit")
    @Operation(summary = "退出位置共享", description = "用户退出指定的位置共享，若所有成员退出则自动关闭共享")
    public R<Void> exit(
        @Parameter(description = "位置共享ID", in = ParameterIn.PATH, required = true) @PathVariable Long shareId,
        @Parameter(description = "退出信息", required = true) @Valid @RequestBody LocationShareExitVo vo) {
        locationShareService.exitLocationShare(shareId, vo);
        return R.success();
    }
}
