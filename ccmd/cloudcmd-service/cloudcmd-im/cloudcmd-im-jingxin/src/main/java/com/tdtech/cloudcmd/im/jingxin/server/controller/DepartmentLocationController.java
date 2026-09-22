package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DepartmentLocation;
import com.tdtech.cloudcmd.im.jingxin.server.service.DepartmentLocationService;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 部门位置信息表 前端控制器
 * </p>
 *
 * @since 2025-08-13
 */
@RestController
@Slf4j
@Tag(name = "位置")
@RequestMapping("/collaboration/v1/dept/location")
public class DepartmentLocationController {

    @Autowired
    private DepartmentLocationService deptLocationService;

    @Autowired
    private ReportUtil reportUtil;

    /**
     * 分页查询部门位置列表
     */
    @GetMapping("/list")
    public ListResult getList(@RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String deptName) {

        IPage<DepartmentLocation> page = deptLocationService.getPageList(pageNum, pageSize, deptName);
        return new ListResult(page.getTotal(), page.getRecords());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ListResult {
        private Long total;
        private List<DepartmentLocation> list;
    }

    /**
     * 保存部门位置信息（新增或编辑）
     */
    @PostMapping("/save")
    public R save(@RequestBody DepartmentLocation departmentLocation) {
        try {
            if (Objects.nonNull(departmentLocation.getId())) {
                return R.success(deptLocationService.updateLocation(departmentLocation));
            } else {
                return R.success(deptLocationService.saveLocation(departmentLocation));
            }
        } catch (Exception e) {
            log.error("保存部门位置失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.LOCATION_INSERT, "保存部门位置失败：" + e.getMessage());
            return R.failure(e.getMessage());
        }
    }

    /**
     * 删除部门位置信息
     */
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable Long id) {
        try {
            DepartmentLocation old = deptLocationService.getById(id);
            if (Objects.isNull(old)) {
                reportUtil.saveOperationLog(OperationTypeEnum.LOCATION_DELETE, "删除部门位置：[id=" + id + "]不存在");
                return R.failure("部门位置信息不存在");
            }
            return R.success(deptLocationService.deleteDepartmentLocation(old));
        } catch (Exception e) {
            log.error("删除部门位置失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * 根据ID查询部门位置信息（用于编辑）
     */
    @GetMapping("/get/{id}")
    public R getById(@PathVariable Long id) {
        return R.success(deptLocationService.getById(id));
    }
}