package com.tdtech.cloudcmd.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomDutyScheduleVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomUserCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomUserVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomVO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.entity.DepartmentCustom;
import com.tdtech.cloudcmd.auth.entity.DepartmentNodeCustom;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.service.IDepartmentCustomService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/v1/custom-department")
@Tag(name = "自定义通讯录管理", description = "自定义通讯录、节点和成员管理接口")
@CrossOrigin
public class DepartmentCustomController {

    private final IDepartmentCustomService departmentCustomService;

    public DepartmentCustomController(IDepartmentCustomService departmentCustomService) {
        this.departmentCustomService = departmentCustomService;
    }

    @GetMapping("/tree")
    @Operation(summary = "查询自定义通讯录列表")
    public R<List<DepartmentCustomVO>> listTrees(@RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.listTrees(imUserId));
    }

    @GetMapping("/tree/{id}")
    @Operation(summary = "查询自定义通讯录详情")
    public R<DepartmentCustom> detailTree(@PathVariable("id") Long id,
                                          @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.detailTree(id, imUserId));
    }

    @PostMapping("/tree")
    @Operation(summary = "创建自定义通讯录")
    public R<DepartmentCustom> createTree(@RequestBody DepartmentCustomCO co) {
        return R.success(departmentCustomService.createTree(co));
    }

    @PutMapping("/tree/{id}")
    @Operation(summary = "更新自定义通讯录")
    public R<DepartmentCustom> updateTree(@PathVariable("id") Long id, @RequestBody DepartmentCustomCO co) {
        return R.success(departmentCustomService.updateTree(id, co));
    }

    @DeleteMapping("/tree/{id}")
    @Operation(summary = "删除自定义通讯录")
    public R<Void> deleteTree(@PathVariable("id") Long id) {
        departmentCustomService.deleteTree(id);
        return R.success();
    }

    @GetMapping("/node/tree")
    @Operation(summary = "查询自定义通讯录节点树")
    public R<List<DepartmentNodeCustomVO>> nodeTree(@RequestParam("departmentCustomId") Long departmentCustomId,
                                                    @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.nodeTree(departmentCustomId, imUserId));
    }

    @GetMapping("/children")
    @Operation(summary = "查询自定义通讯录直接子节点")
    public R<List<DepartmentNodeCustomVO>> children(@RequestParam("departmentCustomId") Long departmentCustomId,
                                                    @RequestParam(value = "parentId", required = false) Long parentId,
                                                    @RequestParam(value = "keyword", required = false) String keyword,
                                                    @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.children(departmentCustomId, parentId, keyword, imUserId));
    }

    @GetMapping("/node")
    @Operation(summary = "查询自定义通讯录节点")
    public R<List<DepartmentNodeCustomVO>> listNodes(@RequestParam("departmentCustomId") Long departmentCustomId,
                                                     @RequestParam(value = "parentId", required = false) Long parentId,
                                                     @RequestParam(value = "keyword", required = false) String keyword,
                                                     @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.listNodes(departmentCustomId, parentId, keyword, imUserId));
    }

    @GetMapping("/node/{id}")
    @Operation(summary = "查询自定义通讯录节点详情")
    public R<DepartmentNodeCustom> detailNode(@PathVariable("id") Long id,
                                              @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.detailNode(id, imUserId));
    }

    @PostMapping("/node")
    @Operation(summary = "创建自定义通讯录节点")
    public R<DepartmentNodeCustom> createNode(@RequestBody DepartmentNodeCustomCO co) {
        return R.success(departmentCustomService.createNode(co));
    }

    @PutMapping("/node/{id}")
    @Operation(summary = "更新自定义通讯录节点")
    public R<DepartmentNodeCustom> updateNode(@PathVariable("id") Long id, @RequestBody DepartmentNodeCustomCO co) {
        return R.success(departmentCustomService.updateNode(id, co));
    }

    @DeleteMapping("/node/{id}")
    @Operation(summary = "删除自定义通讯录节点")
    public R<Void> deleteNode(@PathVariable("id") Long id) {
        departmentCustomService.deleteNode(id);
        return R.success();
    }

    @GetMapping("/node/{id}/user")
    @Operation(summary = "查询自定义通讯录节点成员")
    public R<List<DepartmentCustomUserVO>> listUsers(@PathVariable("id") Long id,
                                                     @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.listUsers(id, imUserId));
    }

    @GetMapping("/node/{id}/user/page")
    @Operation(summary = "分页查询自定义通讯录节点成员")
    public R<IPage<DepartmentCustomUserVO>> pageUsers(@PathVariable("id") Long id,
                                                      @RequestParam(defaultValue = "1", value = "pageNum") Long pageNum,
                                                      @RequestParam(defaultValue = "10", value = "pageSize") Long pageSize,
                                                      @RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.pageUsers(id, pageNum, pageSize, keyword, imUserId));
    }

    @GetMapping("/node/{id}/duty-schedule-user")
    @Operation(summary = "查询自定义通讯录部门下的排班人员")
    public R<List<DepartmentCustomDutyScheduleVO>> listDutyScheduleUsers(@PathVariable("id") Long id,
                                                                         @RequestParam(value = "dutyStartDate", required = false) String dutyStartDate,
                                                                         @RequestParam(value = "dutyEndDate", required = false) String dutyEndDate,
                                                                         @RequestParam(value = "dutyType", required = false) Long dutyType,
                                                                         @RequestParam(value = "imUserId", required = false) Long imUserId) {
        return R.success(departmentCustomService.listDutyScheduleUsers(id, dutyStartDate, dutyEndDate, dutyType,
                imUserId));
    }

    @PostMapping("/node/{id}/user")
    @Operation(summary = "添加自定义通讯录节点成员")
    public R<Void> addUsers(@PathVariable("id") Long id, @RequestBody DepartmentCustomUserCO co) {
        departmentCustomService.addUsers(id, co == null ? null : co.getUsers());
        return R.success();
    }

    @DeleteMapping("/node/{id}/user")
    @Operation(summary = "移除自定义通讯录节点成员")
    public R<Void> deleteUsers(@PathVariable("id") Long id, @RequestParam("userIds") String userIds) {
        departmentCustomService.deleteUsers(id, parseIds(userIds));
        return R.success();
    }

    @GetMapping("/available-user/page")
    @Operation(summary = "分页查询可选警信用户")
    public R<IPage<ImUserDO>> pageAvailableUsers(@RequestParam(defaultValue = "1", value = "pageNum") Long pageNum,
                                                 @RequestParam(defaultValue = "10", value = "pageSize") Long pageSize, ImUserQO qo) {
        return R.success(departmentCustomService.pageAvailableUsers(pageNum, pageSize, qo));
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(ids.split(",")).filter(id -> !id.isBlank()).map(String::trim).map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BusinessException("用户ID列表格式错误");
        }
    }
}
