package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyType;
import com.tdtech.cloudcmd.im.jingxin.server.service.DutyTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "排班类型", description = "排班类型配置管理")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/duty/type")
@RequiredArgsConstructor
public class DutyTypeController {

    @Resource
    private DutyTypeService dutyTypeService;

    @Operation(summary = "分页查询排班类型", description = "根据排班类型名称分页查询排班类型列表")
    @GetMapping("/page")
    public R<IPage<DutyType>> getPageList(
        @Parameter(description = "当前页码", example = "1") @RequestParam(required = false) Integer pageNum,
        @Parameter(description = "每页大小", example = "10") @RequestParam(required = false) Integer pageSize,
        @Parameter(description = "排班类型名称，支持模糊查询", example = "白班") @RequestParam(required = false) String name) {
        return R.success(dutyTypeService.getPageList(pageNum, pageSize, name));
    }

    @Operation(summary = "查询全部排班类型", description = "查询所有可用的排班类型列表")
    @GetMapping("/all")
    public R<List<DutyType>> getAllList() {
        return R.success(dutyTypeService.getAllList());
    }

    @Operation(summary = "查询排班类型详情", description = "根据排班类型标识查询排班类型详情，type 即排班类型主键")
    @GetMapping("/{type}")
    public R<DutyType> getByType(
        @Parameter(description = "排班类型标识", required = true, example = "1") @PathVariable Long type) {
        return R.success(dutyTypeService.getDutyTypeByType(type));
    }

    @Operation(summary = "新增排班类型", description = "新增排班类型，类型标识和名称不能为空")
    @PostMapping
    public R<Boolean> create(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "排班类型信息", required = true)
        @RequestBody DutyType dutyType) {
        return R.success(dutyTypeService.saveDutyType(dutyType));
    }

    @Operation(summary = "修改排班类型", description = "根据排班类型标识修改排班类型信息，type 即排班类型主键")
    @PutMapping("/{type}")
    public R<Boolean> update(
        @Parameter(description = "排班类型标识", required = true, example = "1") @PathVariable Long type,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "排班类型信息", required = true)
        @RequestBody DutyType dutyType) {
        dutyType.setType(type);
        return R.success(dutyTypeService.saveDutyType(dutyType));
    }

    @Operation(summary = "删除排班类型", description = "根据排班类型标识删除排班类型，已被排班信息引用时不能删除")
    @DeleteMapping("/{type}")
    public R<Boolean> delete(
        @Parameter(description = "排班类型标识", required = true, example = "1") @PathVariable Long type) {
        return R.success(dutyTypeService.deleteDutyType(type));
    }
}
