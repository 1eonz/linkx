package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentService;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 职能部门控制器
 */
@Tag(name = "职能部门", description = "职能部门相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/functionaldepts")
@RequiredArgsConstructor
public class FunctionalDepartmentController {

    @Resource
    private FunctionalDepartmentService functionalDepartmentService;

    @PostMapping()
    @Operation(summary = "创建职能部门节点", description = "创建职能部门节点")
    @LogReport(type = OperationTypeEnum.COLLABORATION_FUNCTIONALDEPTS_INSERT)
    public R<Boolean> createFunctionalDepartmentNode(@LogReportParam(field = "name") @RequestBody FunctionalDepartmentReq functionalDepartmentReq) {
        return R.success(functionalDepartmentService.createFunctionalDepartmentNode(functionalDepartmentReq));
    }

    @PutMapping("/{departmentId}")
    @Operation(summary = "修改职能部门节点", description = "修改职能部门节点")
    public R<Boolean> updateFunctionalDepartmentNode(@PathVariable String departmentId,
                                                     @RequestBody FunctionalDepartmentReq functionalDepartmentReq) {
        return R.success(functionalDepartmentService.updateFunctionalDepartmentNode(departmentId, functionalDepartmentReq));
    }

    @DeleteMapping("/{departmentId}")
    @Operation(summary = "删除职能部门节点", description = "删除职能部门节点")
    public R<Boolean> deleteFunctionalDepartmentNode(@PathVariable String departmentId) {
        return R.success(functionalDepartmentService.deleteFunctionalDepartmentNode(departmentId));
    }

    @GetMapping("/{departmentId}/children")
    @Operation(summary = "查询指定职能部门的子部门", description = "查询指定职能部门的子部门")
    public R<List<FunctionalDepartment>> childrenDepartmentNode(@PathVariable String departmentId) {
        return R.success(functionalDepartmentService.childrenDepartmentNode(departmentId));
    }
}