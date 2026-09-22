package com.tdtech.cloudcmd.admin.resource.controller;

import com.tdtech.cloudcmd.admin.resource.entity.dto.PerWarningRalationDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.service.IPerWarningRalationService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;


/**
 * @author: S063874
 * @date: 2026-01-13 15:29
 */
@Slf4j
@Tag(name = "预警推送")
@RestController
@RequestMapping("/admin/v1/warning/ralation")
public class PerWarningRalationController {


    @Resource
    private IPerWarningRalationService perWarningRalationService;


    @PostMapping("/create")
    @Operation(summary = "创建预警推送")
    public R<Integer> createPerWarningRalation(@RequestBody PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        return R.success(perWarningRalationService.createPerWaringRalation(perWarningRalationSaveReqVO));
    }
    @PostMapping("/update")
    @Operation(summary = "更新预警推送")
    public R<Boolean> updatePerWarningRalation(@Valid @RequestBody PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        boolean b = perWarningRalationService.updatePerWaringRalation(perWarningRalationSaveReqVO);
        if(!b){
            return R.failure("更新失败，该配置已存在");
        }
        return R.success(true);
    }

    @DeleteMapping("/delete/{id}")
    @Parameter(name = "id", description = "编号", required = true)
    @Operation(summary = "删除预警推送")
    public R<Boolean> deletePerWarningRalation(@PathVariable Long id) {
        perWarningRalationService.deletePerWaringRalation(id);
        return R.success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取预警推送列表")
    public R<PageResult<PerWarningRalationDO>> getPerWarningRalationPage(PerWarningRalationPageReqVO pageReqVO) {
        return R.success(perWarningRalationService.getPerWarningRalationPage(pageReqVO));
    }

    @DeleteMapping("/delete/list")
    @Parameter(name = "ids", description = "编号集合", required = true)
    @Operation(summary = "批量预警推送")
    public R<Boolean> deletePerWaringRalationListByIds(@RequestBody List<Long> ids) {
        perWarningRalationService.deletePerWaringRalationListByIds(ids);
        return R.success(true);
    }
}
