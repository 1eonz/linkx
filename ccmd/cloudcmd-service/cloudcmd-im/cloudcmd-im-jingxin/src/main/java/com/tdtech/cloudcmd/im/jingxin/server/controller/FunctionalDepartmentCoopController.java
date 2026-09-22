package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDeleteVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoop;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentCoopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 职能部门协同岗用户关联控制器
 */
@Tag(name = "职能部门协同岗", description = "职能部门协同岗用户关联相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/functionaldepts")
@RequiredArgsConstructor
public class FunctionalDepartmentCoopController {

    @Resource
    FunctionalDepartmentCoopService functionalDepartmentCoopService;

    @GetMapping("{deptId}/coop")
    @Operation(summary = "查询指定职能部门的协同岗用户", description = "查询指定职能部门的协同岗用户（分页）")
    public R<Page<CoopUser>> getCoopUsers(
            @PathVariable String deptId,
            @Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
            @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
            @Parameter(description = "组织id") @RequestParam(value = "orgId", required = false) Long orgId) {
        return R.success(functionalDepartmentCoopService.getCoopUsers(deptId, pageNum, pageSize, orgId));
    }

    @GetMapping("/coop")
    @Operation(summary = "搜索协同岗用户", description = "搜索协同岗用户（带搜索条件，分页）")
    public R<Page<CoopUser>> searchCoopUsers(
            @Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
            @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
            @Parameter(description = "职能部门ID") @RequestParam(value = "deptId", required = false) Long deptId,
            @Parameter(description = "协同岗名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime", required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false) String endTime) {
        return R.success(functionalDepartmentCoopService.searchCoopUsers(pageNum, pageSize, deptId, name, startTime, endTime));
    }

    @PutMapping("{deptId}/coop")
    @Operation(summary = "修改指定职能部门的协同岗用户", description = "修改指定职能部门的协同岗用户")
    public R<Boolean> putCoopUsers(
            @PathVariable Long deptId,
            @RequestBody List<FunctionalDepartmentCoop> coopUserIds) {
        return R.success(functionalDepartmentCoopService.putCoopUsers(deptId, coopUserIds));
    }

    @PutMapping("/checked/coop")
    @Operation(summary = "修改挂靠协同岗的勾选状态", description = "修改挂靠协同岗的勾选状态")
    public R<Boolean> putCoopUsersChecked(@RequestBody List<FunctionalDepartmentCoopCO> functionalDepartmentCoops) {
        return R.success(functionalDepartmentCoopService.putCoopUsersChecked(functionalDepartmentCoops));
    }

    @DeleteMapping("/coop")
    @Operation(summary = "删除协同岗用户关联", description = "删除协同岗用户关联")
    public R<Boolean> deleteCoopUsers(@RequestBody List<FunctionalDeleteVO> functionalDeleteVOS) {
        return R.success(functionalDepartmentCoopService.deleteCoopUsers(functionalDeleteVOS));
    }
}