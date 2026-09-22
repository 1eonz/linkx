package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrant;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrantVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ServerVersion;
import com.tdtech.cloudcmd.im.jingxin.server.service.ApplicationGrantService;
import com.tdtech.cloudcmd.im.jingxin.server.service.OpenAPIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 系统应用授权信息接口（操作新表 linkx_open.tb_application_grant）
 *
 * @author lsc
 * @date 2025/8/11
 **/
@Tag(name = "应用授权管理")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/")
@RequiredArgsConstructor
public class ApplicationGrantController {

    @Resource
    private ApplicationGrantService applicationGrantService;

    @Resource
    private OpenAPIService openAPIService;

    @Operation(summary = "新建应用授权")
    @PostMapping("/client/create")
    public R<Long> create(@RequestBody ApplicationGrantVO vo) {
        try {
            return R.success(applicationGrantService.createApplicationGrant(vo));
        } catch (Exception e) {
            log.error("create application grant failed", e);
            return R.failure(e.getMessage());
        }
    }

    @Operation(summary = "分页查询应用授权")
    @PostMapping("/client/list")
    public R<?> list(@RequestBody(required = false) ApplicationGrantVO vo) {
        if (vo == null) {
            vo = new ApplicationGrantVO();
        }
        return R.success(applicationGrantService.pageApplicationGrant(vo));
    }

    @Operation(summary = "应用授权详情")
    @GetMapping("/client/detail")
    public R<ApplicationGrant> detail(@RequestParam("id") Long id) {
        return R.success(applicationGrantService.getApplicationGrant(id));
    }

    @Operation(summary = "更新应用授权")
    @PutMapping("/client/update")
    public R<Void> update(@RequestBody ApplicationGrantVO vo) {
        try {
            applicationGrantService.updateApplicationGrant(vo);
            return R.success();
        } catch (Exception e) {
            log.error("update application grant failed", e);
            return R.failure(e.getMessage());
        }
    }

    @Operation(summary = "删除应用授权")
    @DeleteMapping("/client/delete")
    public R<Void> delete(@RequestParam("id") Long id) {
        try {
            applicationGrantService.deleteApplicationGrant(id);
            return R.success();
        } catch (Exception e) {
            log.error("删除应用授权失败", e);
            return R.failure(e.getMessage());
        }
    }

    @Operation(summary = "获取服务版本")
    @GetMapping("/base/version")
    public R<ServerVersion> getServerVersion() {
        try {
            return R.success(openAPIService.getServerVersion());
        } catch (Exception e) {
            log.error("getServerVersion error", e);
        }
        return null;
    }
}