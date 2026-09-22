package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.service.UserGroupCareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "收藏")
@RestController
@RequestMapping("/collaboration/v1/cares")
public class UserGroupCareController {

    @Resource
    private UserGroupCareService userGroupCareService;

    /**
     * 关注群组
     */
    @PostMapping
    public R<Boolean> careGroup(@RequestParam Long userId, @RequestParam Long groupId) {
        try {
            return R.success(userGroupCareService.careGroup(userId, groupId));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 取消关注群组
     */
    @DeleteMapping
    public R<Boolean> uncareGroup(@RequestParam Long userId, @RequestParam Long groupId) {
        try {
            return R.success(userGroupCareService.uncareGroup(userId, groupId));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 批量取消关注群组
     */
    @DeleteMapping("/batch")
    public R<Boolean> batchUncareGroups(@RequestParam Long userId, @RequestBody List<Long> groupIds) {
        try {
            return R.success(userGroupCareService.batchUncareGroups(userId, groupIds));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 查询用户关注的群组ID列表
     */
    @GetMapping("/user/{userId}")
    public R<List<Long>> getCaredGroupIds(@PathVariable Long userId) {
        try {
            return R.success(userGroupCareService.getCaredGroupIds(userId));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 分页查询用户关注的群组
     */
    @GetMapping("/user/{userId}/page")
    public R<IPage<Long>> getCaredGroupsPage(@PathVariable Long userId,
        @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            return R.success(userGroupCareService.getCaredGroupsPage(userId, pageNum, pageSize));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    /**
     * 检查是否已关注群组
     */
    @GetMapping("/check")
    public R<Boolean> checkIsCared(@RequestParam Long userId, @RequestParam Long groupId) {
        try {
            return R.success(userGroupCareService.checkIsCared(userId, groupId));
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }
}
