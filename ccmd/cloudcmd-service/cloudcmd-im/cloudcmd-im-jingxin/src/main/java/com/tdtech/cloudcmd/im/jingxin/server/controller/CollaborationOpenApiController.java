package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClientVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ServerVersion;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationOpenApiService;
import com.tdtech.cloudcmd.im.jingxin.server.service.OpenAPIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author lsc
 * @date 2025/8/11 对外三方接口
 *
 * 注：原 /collaboration/v1/client/* 路径已迁移至 {@link ApplicationGrantController}，操作新表 linkx_open.tb_application_grant。
 * 本 controller 保留老接口用于过渡（操作老表 icp_collabs.tb_collaboration_client）。
 **/
@Tag(name = "OPEN API 对接配置（旧）")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/old/")
@RequiredArgsConstructor
public class CollaborationOpenApiController {

    @Resource
    private OpenAPIService openAPIService;

    @Resource
    private CollaborationOpenApiService collaborationOpenApiService;

    /**
     * 管理三方客户端账号密码
     *
     * @return
     */
    @PostMapping("/client/create")
    public R createClient(@RequestBody CollaborationClientVO collaborationClientVO) {
        try {
            return R.success(collaborationOpenApiService.createClient(collaborationClientVO));
        } catch (Exception e) {
            log.error("创建应用失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * 获取三方客户端账号密码
     *
     * @return
     */
    @PostMapping("/client/list")
    public R getClient(@RequestBody(required = false) CollaborationClientVO collaborationClientVO) {
        return R.success(collaborationOpenApiService.getClient(collaborationClientVO));
    }

    /**
     * 删除三方客户端账号密码
     *
     * @return
     */
    @DeleteMapping("/client/delete")
    public R deleteClient(@RequestParam("id") Long id) {
        collaborationOpenApiService.deleteClient(id);
        return R.success();
    }

    /**
     * 修改三方客户端账号密码
     */
    @PutMapping("/client/update")
    public R updateClient(@RequestBody CollaborationClientVO collaborationClientVO) {
        try {
            collaborationOpenApiService.updateClient(collaborationClientVO);
            return R.success();
        } catch (Exception e) {
            log.error("更新应用失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/client/detail")
    public R clientDetail(@RequestParam("id") Long id) {
        return R.success(collaborationOpenApiService.clientDetail(id));
    }


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
