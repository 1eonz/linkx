package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketType;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketTypeQO;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

@Slf4j
@RequestMapping("/collaboration/v1/policetickettype")
@RestController
@Tag(name = "警单类型", description = "提供警单类型管理功能")
@RequiredArgsConstructor
public class PoliceTicketTypeController {

    private final PoliceTicketTypeService policeTicketTypeService;

    /**
     * 创建警单类型
     */
    @PostMapping
    @Operation(summary = "创建警单类型", description = "创建一个新的警单类型")
    public R<Void> create(
        @RequestBody @Parameter(name = "policeTicketType", description = "警单类型对象") PoliceTicketType policeTicketType) {
        policeTicketTypeService.create(policeTicketType);
        return R.success();
    }

    /**
     * 根据ID查询警单类型
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询警单类型", description = "根据ID查询警单类型详情")
    public R<PoliceTicketType> findById(
        @PathVariable @Parameter(name = "id", description = "警单类型ID") Long id) {
        return R.success(policeTicketTypeService.findById(id));
    }

    /**
     * 更新警单类型
     */
    @PutMapping
    @Operation(summary = "更新警单类型", description = "更新警单类型信息")
    public R<Void> update(
        @RequestBody @Parameter(name = "policeTicketType", description = "警单类型对象") PoliceTicketType policeTicketType) {
        policeTicketTypeService.update(policeTicketType);
        return R.success();
    }

    /**
     * 根据ID删除警单类型
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除警单类型", description = "根据ID删除警单类型")
    public R<Void> deleteById(
        @PathVariable @Parameter(name = "id", description = "警单类型ID") Long id) {
        policeTicketTypeService.deleteById(id);
        return R.success();
    }

    /**
     * 查询所有警单类型
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有警单类型", description = "查询所有警单类型列表")
    public R<List<PoliceTicketType>> findAll(PoliceTicketTypeQO policeTicketTypeQO) {
        return R.success(policeTicketTypeService.findAll(policeTicketTypeQO));
    }

    /**
     * 分页查询警单类型
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询警单类型", description = "分页查询警单类型列表")
    public R<Page<PoliceTicketType>> findPage(
        @RequestParam(defaultValue = "1") @Parameter(name = "pagenum", description = "页码，默认为1") Long pagenum,
        @RequestParam(defaultValue = "10") @Parameter(name = "pagesize", description = "每页大小，默认为10") Long pagesize,
        @Parameter(name = "policeTicketType", description = "警单类型查询条件") PoliceTicketType policeTicketType) {
        Page<PoliceTicketType> page = new Page<>(pagenum, pagesize);
        return R.success(policeTicketTypeService.findPage(page, policeTicketType));
    }

}
