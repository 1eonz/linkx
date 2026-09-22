package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualCreateReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImUserVirtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@Tag(name = "虚拟用户管理")
@RestController
@RequestMapping("/collaboration/v1/im/users")
@RequiredArgsConstructor
@Validated
public class ImUserVirtualController {

    @Resource
    private ImUserVirtualService imUserVirtualService;

    @PostMapping("/virtual")
    @Operation(summary = "创建虚拟用户", description = "创建虚拟用户")
    public R<Void> createVirtualUser(@Valid @RequestBody ImUserVirtualCreateReq req) {
        imUserVirtualService.createVirtualUser(req);
        return R.success();
    }

    @PutMapping("/virtual/{userId}")
    @Operation(summary = "更新虚拟用户", description = "更新虚拟用户")
    @Parameter(name = "userId", description = "虚拟用户ID", required = true)
    public R<Void> updateVirtualUser(@PathVariable Long userId,
                                     @Valid @RequestBody ImUserVirtualUpdateReq req) {
        imUserVirtualService.updateVirtualUser(userId, req);
        return R.success();
    }

    @GetMapping("/virtual")
    @Operation(summary = "查询虚拟用户列表", description = "查询虚拟用户列表")
    public R<List<ImUserVirtualRespVO>> listVirtualUsers(@RequestParam(value = "userName", required = false) String userName){
        return R.success(imUserVirtualService.listVirtualUsers(userName));
    }

    @DeleteMapping("/virtual/{userId}")
    @Operation(summary = "删除虚拟用户", description = "删除虚拟用户")
    @Parameter(name = "userId", description = "虚拟用户ID", required = true)
    public R<Boolean> deleteVirtualUser(@PathVariable Long userId) {
        try {
            var user = imUserVirtualService.getById(userId);
            if (user != null && user.getDeleted().equals(0)) {
                imUserVirtualService.deleteVirtualUserById(user);
            }
        } catch (Exception e) {
            log.error("删除虚拟用户失败", e);
            return R.failure("删除虚拟用户失败");
        }
        return R.success();
    }
}
