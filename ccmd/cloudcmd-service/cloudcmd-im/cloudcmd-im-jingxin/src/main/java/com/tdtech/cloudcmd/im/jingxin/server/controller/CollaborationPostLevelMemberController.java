package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.service.CoopLevelMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "协同岗层级用户", description = "协同岗层级用户相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/cooplevels")
@RequiredArgsConstructor
public class CollaborationPostLevelMemberController {

    @Resource
    CoopLevelMemberService coopLevelMemberService;

    @GetMapping("{levelId}/member")
    @Operation(summary = "查询指定协同岗层级的用户", description = "查询指定协同岗层级的用户")
    public R<Page<CoopUser>> getMembers(@PathVariable String levelId,
                                        @Parameter(description = "页码", required = true) @RequestParam("pageNum") Long pageNum,
                                        @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Long pageSize,
                                        @Parameter(description = "组织id") @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        return R.success(coopLevelMemberService.getMembers(levelId, pageNum, pageSize, orgId));
    }

    @GetMapping("/member")
    @Operation(summary = "查询指定协同岗层级的用户", description = "查询指定协同岗层级的用户")
    public R<Page<CoopUser>> searchMembers(@Parameter(description = "页码", required = true) @RequestParam("pageNum") Long pageNum,
                                           @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Long pageSize,
                                           @Parameter(description = "层级id") @RequestParam(value = "levelId", required = false) String levelId,
                                           @Parameter(description = "协同岗名称") @RequestParam(value = "name") String name,
                                           @Parameter(description = "组织id") @RequestParam(value = "orgId", required = false) Long orgId,
                                           @Parameter(description = "开始时间") @RequestParam(value = "startTime", required = false) String starTime,
                                           @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false) String endTime) {

        return R.success(coopLevelMemberService.searchMembers(name, levelId, orgId, pageNum, pageSize, starTime, endTime));
    }

    @PutMapping("{levelId}/member")
    @Operation(summary = "修改指定协同岗层级的用户", description = "修改指定协同岗层级的用户")
    public R<Boolean> putMembers(@PathVariable String levelId, @RequestBody List<String> coopUserIds) {

        return R.success(coopLevelMemberService.putMembers(levelId, coopUserIds));
    }

    @DeleteMapping("/member")
    @Operation(summary = "删除指定协同岗层级的用户", description = "删除指定协同岗层级的用户")
    public R<Boolean> deleteMembers(@RequestBody List<String> ids) {

        return R.success(coopLevelMemberService.deleteMembers(ids));
    }
}
