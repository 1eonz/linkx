package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefault;
import com.tdtech.cloudcmd.im.jingxin.server.entity.FunctionalDepartmentCoopDefaultReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.FunctionalDepartmentCoopDefaultService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.OrganizationDiversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 部门协同岗用户关联默认表控制器
 */
@Tag(name = "默认协同岗用户", description = "默认协同岗用户相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/functionaldepts/default/coop")
@RequiredArgsConstructor
public class FunctionalDepartmentCoopDefaultController {

    @Resource
    FunctionalDepartmentCoopDefaultService functionalDepartmentCoopDefaultService;


    @GetMapping("/list")
    @Operation(summary = "查询所有默认协同岗用户", description = "查询所有默认协同岗用户")
    public R<List<FunctionalDepartmentCoopDefault>> listAll() {
        return R.success(functionalDepartmentCoopDefaultService.listAll());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询默认协同岗", description = "查询所有默认协同岗用户")
    public R<Page<CoopUser>> listPage(@Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
                                      @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
                                      @Parameter(description = "协同岗名称") @RequestParam(value = "name", required = false) String name,
                                      @Parameter(description = "组织id") @RequestParam(value = "orgId", required = false) Long orgId) {
        return R.success(functionalDepartmentCoopDefaultService.listPage(pageNum, pageSize, name , orgId));
    }

    @PostMapping()
    @Operation(summary = "创建默认协同岗用户", description = "创建默认协同岗用户")
    public R<Boolean> create(@RequestBody FunctionalDepartmentCoopDefaultReq req) {
        return R.success(functionalDepartmentCoopDefaultService.create(req));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量创建默认协同岗用户", description = "批量创建默认协同岗用户")
    public R<Boolean> batchCreate(@RequestBody FunctionalDepartmentCoopDefaultReq req) {
        return R.success(functionalDepartmentCoopDefaultService.batchCreate(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新默认协同岗用户", description = "更新默认协同岗用户")
    public R<Boolean> update(
            @PathVariable String id,
            @RequestBody FunctionalDepartmentCoopDefaultReq req) {
        return R.success(functionalDepartmentCoopDefaultService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除默认协同岗用户", description = "删除默认协同岗用户")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(functionalDepartmentCoopDefaultService.delete(id));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除默认协同岗用户", description = "批量删除默认协同岗用户")
    public R<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return R.success(functionalDepartmentCoopDefaultService.batchDelete(ids));
    }
}