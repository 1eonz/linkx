package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroup;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupGlassesService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author: S063874
 * @date: 2026-02-26 16:42
 */
@Slf4j
@Tag(name = "用户眼镜绑定")
@RestController
@RequestMapping("/collaboration/v1/glasses")
public class GroupGlassesController {

    @Resource
    private GroupGlassesService groupGlassesService;

    /**
     * 获取用户绑定群组信息
     * @param groupId
     * @return GroupGlasses
     */
    @GetMapping("/groupinfo")
    public R<CreateGroup> getGropInfoByGroupId(@RequestParam("groupId") Long groupId) {
        return R.success(groupGlassesService.getGropInfoByGroupId(groupId));
    }
}
