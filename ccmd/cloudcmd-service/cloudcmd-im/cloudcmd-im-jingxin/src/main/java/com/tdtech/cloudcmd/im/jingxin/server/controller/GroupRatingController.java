package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupRatingReqDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingListRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingStatusRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupRatingService;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 群组评价控制器
 * 提供群组协同岗评价功能
 *
 * @author ChinasoftPortal
 * @date 2025/9/10
 */
@Slf4j
@Tag(name = "群组评价")
@RestController
@RequestMapping("/collaboration/v1/groups")
public class GroupRatingController {

    @Resource
    private GroupRatingService groupRatingService;

    /**
     * 查询当前用户对指定群组协同岗的已评价列表
     *
     * @param groupId 群组ID
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 评价列表响应
     */
    @Operation(summary = "查询我对指定群组协同岗的已评价列表")
    @GetMapping("/{groupId}/rating/list")
    public R<GroupRatingListRespVO> getRatedList(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        Long currentUserId = user.getUserId();
        return R.success(groupRatingService.getRatedList(groupId, pageNum, pageSize, currentUserId));
    }

    /**
     * 对已归档群组的协同岗发起评分
     * 只能对已归档的群组进行评价，且不能重复评价同一个协同岗
     *
     * @param groupId 群组ID
     * @param reqDTO 评价请求对象
     * @return 评价响应
     */
    @Operation(summary = "对群组协同岗发起评分")
    @PostMapping("/{groupId}/rating")
    public R<GroupRatingRespVO> createRating(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupRatingReqDTO reqDTO) {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        Long currentUserId = user.getUserId();
        return R.success(groupRatingService.createRating(groupId, reqDTO, currentUserId));
    }

    /**
     * 查询指定群组的协同岗列表
     *
     * @param groupId 群组ID
     * @return 协同岗列表
     */
    @Operation(summary = "查询指定群组的协同岗列表")
    @GetMapping("/{groupId}/coop/list")
    public R<List<CoopUserVO>> getCoopUserList(@PathVariable Long groupId) {
        return R.success(groupRatingService.getCoopUserList(groupId));
    }

    /**
     * 查询当前用户对指定群组的评价状态
     * 包含已评价数量、总协同岗数量、是否完成所有评价
     *
     * @param groupId 群组ID
     * @return 评价状态响应
     */
    @Operation(summary = "查询当前用户对指定群组的评价状态")
    @GetMapping("/{groupId}/rating/status")
    public R<GroupRatingStatusRespVO> getRatingStatus(@PathVariable Long groupId) {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        Long currentUserId = user.getUserId();
        return R.success(groupRatingService.getRatingStatus(groupId, currentUserId));
    }
}
