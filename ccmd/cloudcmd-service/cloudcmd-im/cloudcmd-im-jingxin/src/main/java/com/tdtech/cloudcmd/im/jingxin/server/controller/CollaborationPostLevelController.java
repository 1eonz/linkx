package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevel;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelReq;
import com.tdtech.cloudcmd.im.jingxin.server.service.CoopLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "协同岗层级", description = "协同岗层级相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/cooplevels")
@RequiredArgsConstructor
public class CollaborationPostLevelController {

    @Resource
    CoopLevelService coopLevelService;

    @PostMapping()
    @Operation(summary = "创建协同岗层级节点", description = "创建协同岗层级节点")
    public R<Boolean> createCoopLevelNode(@RequestBody CoopLevelReq coopLevelReq) {

        return R.success(coopLevelService.createCoopLevelNode(coopLevelReq));
    }

    @PutMapping("/{levelId}")
    @Operation(summary = "修改协同岗层级节点", description = "修改协同岗层级节点")
    public R<Boolean> updateCooLevelNode(@PathVariable String levelId, @RequestBody CoopLevelReq coopLevelReq) {

        return R.success(coopLevelService.updateCoopLevelNode(levelId, coopLevelReq));
    }

    @DeleteMapping("/{levelId}")
    @Operation(summary = "删除协同岗层级节点", description = "删除协同岗层级节点")
    public R<Boolean> deleteCooLevelNode(@PathVariable String levelId) {

        return R.success(coopLevelService.deleteCoopLevelNode(levelId));
    }

    @GetMapping("/{levelId}/children")
    @Operation(summary = "查询指定协同岗层级的子层级", description = "查询指定协同岗层级的子层级")
    public R<List<CoopLevel>> childrenLevelNode(@PathVariable String levelId) {

        return R.success(coopLevelService.childrenLevelNode(levelId));
    }
}
