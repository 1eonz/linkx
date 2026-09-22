package com.tdtech.cloudcmd.linkx.third.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.linkx.third.api.dto.*;
import com.tdtech.cloudcmd.linkx.third.service.AppGroupService;
import com.tdtech.cloudcmd.linkx.third.vo.AppGroupDetailVo;
import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 应用分组管理 Controller
 *
 * @author wb
 * @since 2026-05-09
 */
@Tag(name = "应用分组管理", description = "应用分组相关接口")
@RestController
@RequestMapping("/third/v1/apps")
@RequiredArgsConstructor
public class AppGroupController {
    @Autowired
    private AppGroupService appGroupService;

    @PostMapping("/groups/app")
    @Operation(summary = "创建三方应用分组(含绑定应用)", description = "创建三方应用分组(含绑定应用)")
    public R<Void> createContainsApp(@Valid @RequestBody AppGroupCreateDto appGroupCreateVo) {
        appGroupService.createContainsApp(appGroupCreateVo);
        return R.success();
    }
    @PostMapping("/groups")
    @Operation(summary = "创建三方应用分组", description = "创建三方应用分组")
    public R<Void> create(@Valid @RequestBody AppGroupCreateDto appGroupCreateVo) {
        appGroupService.create(appGroupCreateVo);
        return R.success();
    }

    @DeleteMapping("/groups/{id}")
    @Operation(summary = "删除应用分组", description = "删除应用分组")
    @Parameter(name = "id", description = "分组ID", required = true)
    public R<Void> deleteById(@PathVariable Long id) {
        appGroupService.deleteGroups(id);
        return R.success();
    }

    @PutMapping("/groups/app")
    @Operation(summary = "更新分组信息(含修改应用)", description = "更新(含修改应用)")
    public R<Void> updateContainsApp(@Valid @RequestBody AppGroupUpdateDto updateDto) {
        appGroupService.updateContainsApp(updateDto);
        return R.success();
    }

    @PutMapping("/groups")
    @Operation(summary = "更新分组信息", description = "更新分组信息")
    public R<Void> updateSort(@Valid @RequestBody AppGroupUpdateDto updateDto) {
        appGroupService.update(updateDto);
        return R.success();
    }


    @GetMapping("/groups")
    @Operation(summary = "获取应用分组列表", description = "获取应用分组列表")
    @Parameter(name = "type", description = "分组类型：1系统级 2用户级", required = true)
    @Parameter(name = "userId", description = "用户id,用户级时必传")
    @Parameter(name = "scope", description = "应用展示范围。1：鸿蒙移动端；2：安卓移动端；4：PC浏览器；8：PC桌面端，")
    public R<List<AppGroupDetailVo>> getGroupsDetails(@RequestParam(value = "type") Integer type,
                                                      @RequestParam(value = "userId", required = false) Long userId,
                                                      @RequestParam(value = "scope", required = false) Integer scope) {
        return R.success(appGroupService.getGroupsDetails(type, userId ,scope));
    }

    @PutMapping("/{groupId}/groups")
    @Operation(summary = "绑定应用到分组", description = "绑定应用到分组")
    @Parameter(name = "appId", description = "应用id", required = true)
    public R<Void> appAddToGroup(@PathVariable(value = "groupId") Long groupId,
                                 @Valid @RequestBody AppToGroupDto appToGroupDto) {
        appGroupService.appAddToGroup(groupId, appToGroupDto);
        return R.success();
    }

    @DeleteMapping("/{groupId}/groups")
    @Operation(summary = "删除分组中的应用", description = "删除分组中的应用")
    @Parameter(name = "appId", description = "应用id", required = true)
    public R<Void> deleteAppFromGroup(@PathVariable(value = "groupId") Long groupId,
                                      @Valid @RequestBody AppToGroupDto appToGroupDto) {
        appGroupService.deleteAppFromGroup(groupId, appToGroupDto);
        return R.success();
    }

    @PostMapping("/{appId}/used")
    @Operation(summary = "创建应用使用记录", description = "创建应用使用记录")
    @Parameter(name = "appId", description = "应用id", required = true)
    public R<Void> createUsed(@PathVariable(value = "appId") Long appId,
                              @Valid @RequestBody AppUsedDto appUsedDto) {
        appGroupService.createUsed(appId, appUsedDto);
        return R.success();
    }

    @GetMapping("/used")
    @Operation(summary = "获取应用使用记录排行", description = "获取应用使用记录排行")
    @Parameter(name = "userId", description = "用户id")
    public R<List<AppUsedRankingVo>> getAppUsedRanking(@RequestParam(value = "userId") Long userId,
                                                       @RequestParam(value = "terminalType", required = false) Integer terminalType,
                                                       @RequestParam(value = "scope", required = false) Integer scope) {
        return R.success(appGroupService.getAppUsedRanking(userId, terminalType, scope));
    }
}
