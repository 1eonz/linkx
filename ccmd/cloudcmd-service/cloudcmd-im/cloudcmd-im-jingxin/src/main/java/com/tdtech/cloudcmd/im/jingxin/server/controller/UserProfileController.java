package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;

import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileVo;
import com.tdtech.cloudcmd.im.jingxin.server.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Tag(name = "用户偏好信息")
@RestController
@RequestMapping("/collaboration/v1")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("/apps/sortType")
    @Operation(summary = "创建App展示方式", description = "创建App展示方式")
    public R<Void> createOrUpdate(@Valid @RequestBody UserProfileCreateReq createReq) {
        userProfileService.createOrUpdate(createReq);
        return R.success();
    }

    @GetMapping("/apps/sortType")
    @Operation(summary = "获取App展示方式", description = "获取App展示方式")
    public R<Integer> getSortedType(@RequestParam(value = "userId") Long userId) {
        return R.success(userProfileService.getSortedType(userId));
    }

    @GetMapping("/users/profile")
    @Operation(summary = "获取用户偏好信息", description = "获取用户偏好信息")
    public R<UserProfileVo> getProfile(@RequestParam(value = "userId") Long userId,
                                       @RequestParam(value = "terminalType", required = false) Integer terminalType,
                                       @RequestParam(value = "scope", required = false) Integer scope) {
        return R.success(userProfileService.getProfile(userId, terminalType ,scope));
    }
}
