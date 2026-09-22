package com.tdtech.cloudcmd.admin.resource.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppRespVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.service.UserCommonAppService;
import com.tdtech.cloudcmd.bean.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@Tag(name = "用户常用应用")
@RestController
@RequestMapping("/admin/v1/content/app/common")
@Validated
@CrossOrigin
public class UserCommonAppController {

    @Resource(name = "userCommonAppService")
    private UserCommonAppService service;

    @PostMapping("/save")
    @Operation(summary = "创建用户常用应用")
    @PermitAll
    public R<List<Long>> save(@Valid @RequestBody UserCommonAppSaveReqVO createReqVO) {
        return R.success(service.save(createReqVO));
    }

    @GetMapping("/getInitStatus")
    @Operation(summary = "获取用户常用应用初始化状态")
    @PermitAll
    public R<Boolean> getInitStatus(@RequestParam("userId") String userId,
                                    @RequestParam(value = "terminalType", required = false) Integer terminalType) {
        return R.success(service.getInitStatus(userId,terminalType));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户常用应用")
    @Parameter(name = "id", description = "编号", required = true)
    @PermitAll
    public R<Boolean> delete(@RequestParam("id") Long id,
                             @RequestParam(value = "terminalType", required = false) Integer terminalType) {
        service.delete(id, terminalType);
        return R.success(true);
    }

    @GetMapping("/get_my")
    @Operation(summary = "获取当前用户常用应用")
    @PermitAll
    public R<List<UserCommonAppRespVO>> getMy(@RequestParam("userId") String userId,
                                              @RequestParam(value = "terminalType", required = false) Integer terminalType,
                                              @RequestParam(value = "scope", required = false) Integer scope) {
        List<UserCommonAppRespVO> listVO = service.getMy(userId, terminalType, scope);
        return R.success(listVO);
    }
}
