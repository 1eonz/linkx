package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.VirtualUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.ImUserVirtualVO;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.ImVirtualUserVO;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 警信虚拟用户开放接口
 */
@Tag(name = "警信虚拟用户", description = "警信虚拟用户相关接口")
@Slf4j
@RestController
@RequestMapping("/openapi/v1/im/users")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.IM_USER)
@RequestLimit(business = "警信虚拟用户")
public class ImVirtualUserController {

    @DubboReference
    private VirtualUserRpcApi virtualUserRpcApi;

    @Operation(summary = "获取虚拟用户列表", description = "获取系统配置的虚拟用户信息")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @GetMapping("/virtual")
    public R<List<ImVirtualUserVO>> listVirtualUsers() {
        List<ImUserVirtualVO> data = virtualUserRpcApi.listVirtualUsers(null);
        return R.success(BeanCopyUtils.copyList(data, ImVirtualUserVO::new));
    }
}