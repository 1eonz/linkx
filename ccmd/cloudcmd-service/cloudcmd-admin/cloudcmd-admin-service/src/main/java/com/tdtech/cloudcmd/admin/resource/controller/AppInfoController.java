package com.tdtech.cloudcmd.admin.resource.controller;

import com.tdtech.cloudcmd.admin.resource.entity.AppInfo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoUpdateStatusReqVO;
import com.tdtech.cloudcmd.admin.resource.service.IAppInfoService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Objects;
import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Tag(name = "应用")
@RestController
@RequestMapping("/admin/v1/content/app/info")
public class AppInfoController {

    @Resource
    private IAppInfoService infoService;

    @Resource
    private ReportUtil reportUtil;

    @PostMapping("/create")
    @Operation(summary = "创建应用")
    @LogReport(type = OperationTypeEnum.APPLICATION_INSERT)
    public R<Long> createInfo(@Valid @RequestBody @LogReportParam AppInfoSaveReqVO createReqVO) {
        return R.success(infoService.createInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新应用")
    @LogReport(type = OperationTypeEnum.APPLICATION_UPDATE)
    public R<Boolean> updateInfo(@Valid @RequestBody @LogReportParam AppInfoSaveReqVO updateReqVO) {
        infoService.updateInfo(updateReqVO);
        return R.success(true);
    }

    @PutMapping("/update_status")
    @Operation(summary = "更新应用状态")
    public R<Boolean> updateStatus(@Valid @RequestBody AppInfoUpdateStatusReqVO updateStatusVO) {
        if (0 == updateStatusVO.getStatus()) {
            infoService.shelvesAppInfo(updateStatusVO);
        } else if (1 == updateStatusVO.getStatus()) {
            infoService.downShelfAppInfo(updateStatusVO);
        } else {
            reportUtil.saveOperationLog(OperationTypeEnum.APPLICATION_DELETE,
                    "更新应用状态错误status=" + updateStatusVO.getStatus());
        }
        return R.success(true);
    }


    @DeleteMapping("/delete")
    @Operation(summary = "删除应用")
    public R<Boolean> deleteInfo(@RequestParam("id") Long id) {
        try{
            AppInfo old = infoService.findById(id);
            if (Objects.isNull(old)) {
                reportUtil.saveOperationLog(OperationTypeEnum.APPLICATION_DELETE,
                        "删除应用：[id=" + id + "]不存在");
                return R.failure("应用不存在");
            }
            infoService.deleteInfo(old);
            return R.success(true);
        } catch (Exception e){
            return R.failure(e.getMessage());
        }
    }

    @GetMapping("/get")
    @Operation(summary = "获得应用信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public R<AppInfoDO> getInfo(@RequestParam("id") Long id) {
        return R.success(infoService.getInfo(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得应用分页")
    public R<PageResult<AppInfoDO>> getInfoPage(@Valid AppInfoPageReqVO pageReqVO) {
        PageResult<AppInfoDO> pageResult = infoService.getInfoPage(pageReqVO);
        return R.success(pageResult);
    }

    @GetMapping("/apps")
    @Operation(summary = "获得非前置应用分页")
    public R<PageResult<AppInfoDO>> getAppPage(@Valid AppInfoPageReqVO pageReqVO) {
        PageResult<AppInfoDO> pageResult = infoService.getAppPage(pageReqVO);
        return R.success(pageResult);
    }

    @GetMapping("/prerequisite")
    @Operation(summary = "获得前置应用分页")
    public R<PageResult<AppInfoDO>> getPrerequisitePage(@Valid AppInfoPageReqVO pageReqVO) {
        PageResult<AppInfoDO> pageResult = infoService.getPrerequisitePage(pageReqVO);
        return R.success(pageResult);
    }
}