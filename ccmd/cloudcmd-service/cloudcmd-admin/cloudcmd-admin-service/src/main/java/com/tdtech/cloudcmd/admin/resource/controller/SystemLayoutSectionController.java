package com.tdtech.cloudcmd.admin.resource.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tdtech.cloudcmd.admin.resource.entity.SystemLayoutSection;
import com.tdtech.cloudcmd.admin.resource.service.ISystemLayoutSectionService;
import com.tdtech.cloudcmd.bean.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: S063874
 * @date: 2026-03-10 14:00
 */
@Slf4j
@Tag(name = "警务系统版块配置")
@RestController
@RequestMapping("/admin/v1/layout/app")
@Validated
public class SystemLayoutSectionController {

    @Resource
    private ISystemLayoutSectionService systemLayoutSectionService;

    @Resource
    private ReportUtil reportUtil;

    @PostMapping("/sections")
    @Operation(summary = "创建板块")
    @LogReport(type = OperationTypeEnum.GROUP_APPH5_INSERT)
    public R<Boolean> createSection(@Valid @RequestBody @LogReportParam(field = "name") SystemLayoutSection systemLayoutSection) {
        Boolean success = systemLayoutSectionService.createSystemLayoutSection(systemLayoutSection);
        if(success){
            return R.success(success);
        }else{
            return R.failure("该版块已存在!");
        }
    }

    @PutMapping("/sections/{sectionId}")
    @Operation(summary = "修改板块属性")
    @LogReport(type = OperationTypeEnum.GROUP_APPH5_UPDATE)
    @Parameter(name = "sectionId", description = "板块ID", required = true)
    public R<Boolean> updateSection(@PathVariable Long sectionId, @Valid @RequestBody @LogReportParam(field = "name") SystemLayoutSection systemLayoutSection) {
        systemLayoutSection.setId(sectionId);
        Boolean b = systemLayoutSectionService.updateSystemLayoutSection(systemLayoutSection);
        if(b){
            return R.success(true);
        }else {
            return R.failure("该版块已存在!");
        }
    }

    @DeleteMapping("/sections/{sectionId}")
    @Operation(summary = "删除板块")
    @Parameter(name = "sectionId", description = "板块ID", required = true)
    public R<Boolean> deleteSection(@PathVariable Long sectionId) {
        // 查询板块名称用于日志上报
        SystemLayoutSection existing = systemLayoutSectionService.getSystemLayoutSectionById(sectionId);
        String sectionName = existing != null ? existing.getName() : "";
        
        SystemLayoutSection systemLayoutSection = new SystemLayoutSection();
        systemLayoutSection.setId(sectionId);
        systemLayoutSection.setDeleted(1);
        // 逻辑删除
        systemLayoutSectionService.updateSystemLayoutSection(systemLayoutSection);
        
        // 手动上报操作日志
        OperationLog operationLog = new OperationLog(OperationTypeEnum.GROUP_APPH5_DELETE);
        operationLog.setOperation("删除了" + sectionName + "APPH5布局配置");
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);

        return R.success(true);
    }

    @GetMapping("/sections/{sectionId}")
    @Operation(summary = "获取板块详情")
    @Parameter(name = "sectionId", description = "板块ID", required = true)
    public R<SystemLayoutSection> getSection(@PathVariable("sectionId") Long sectionId) {
        SystemLayoutSection section = systemLayoutSectionService.getSystemLayoutSectionById(sectionId);
        return R.success(section);
    }

    @GetMapping("/sections")
    @Operation(summary = "获取板块列表")
    @Parameter(name = "show", description = "是否展示：0-不展示，1-展示")
    public R<List<SystemLayoutSection>> listSections(@RequestParam(value = "show", required = false) Integer show) {
        List<SystemLayoutSection> sections;
        if (show != null && show == 1) {
            sections = systemLayoutSectionService.listShowSystemLayoutSection();
        } else {
            sections = systemLayoutSectionService.listSystemLayoutSection();
        }
        return R.success(sections);
    }
}
