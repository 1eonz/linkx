package com.tdtech.cloudcmd.admin.resource.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Application;
import com.tdtech.cloudcmd.admin.resource.service.IApplicationService;
import com.tdtech.cloudcmd.bean.R;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-28
 */
@Tag(name = "客户端")
@Slf4j
@RestController
@RequestMapping("/admin/v1/application")
public class ApplicationController {
    @Autowired
    private IApplicationService applicationService;

    @PostMapping("/list")
    public R getApplicationList() {
        List<Application> data = applicationService.getApplicationList();
        return R.success(data);
    }

    @PostMapping("/create")
    public R createApplication(@Validated @RequestBody Application application) {
        try {
            applicationService.createApplication(application);
        } catch (AdminException exception) {
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("createApplication error", e);
            return R.failure();
        }
        return R.success();
    }

    @PostMapping("/update")
    public R updateApplication(@Validated @RequestBody Application application) {
        try {
            applicationService.updateApplication(application);
        } catch (AdminException exception) {
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("updateApplication error", e);
            return R.failure();
        }
        return R.success();
    }

    @PostMapping("/delete")
    public R deleteApplication(@RequestBody List<Long> ids) {
        applicationService.deleteApplication(ids);
        return R.success();
    }
}
