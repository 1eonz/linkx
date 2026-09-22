package com.tdtech.cloudcmd.linkx.dashboard.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;
import com.tdtech.cloudcmd.linkx.dashboard.service.IGroupRatingStatService;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 群组评价统计控制器
 *
 * @date: 2026-06-08
 */
@Slf4j
@RestController
@RequestMapping("/dashboard/v1")
@RequiredArgsConstructor
@Tag(name = "群组评价统计", description = "群组评价统计功能")
public class GroupRatingStatController {

    private final IGroupRatingStatService groupRatingStatService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;

    /**
     * 获取协同案件支撑评分（按标签统计）
     *
     * @param type 评分类型（1-直接评分，2-成员评分）
     * @param departmentCode 部门编码（可选）
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd）
     * @param peerId 目标节点 peerId，传入则查询对端节点数据
     * @return 评分统计结果
     */
    @GetMapping("/group/rating/tag")
    @Operation(summary = "获取协同案件支撑评分（按标签统计）", description = "根据时间范围和部门筛选，按标签统计各维度的平均评分")
    public R<GroupRatingStatRespVO> getGroupTagRating(
            @Parameter(description = "评分类型（1-直接评分，2-成员评分）")
            @RequestParam Integer type,
            @Parameter(description = "部门编码（可选）")
            @RequestParam(required = false) String departmentCode,
            @Parameter(description = "开始时间（可选，格式：yyyy-MM-dd）")
            @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（可选，格式：yyyy-MM-dd）")
            @RequestParam(required = false) String endTime,
            @Parameter(description = "目标节点 peerId，传入则查询对端节点数据")
            @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchRating(peerId, "/dashboard/v1/group/rating/tag", type, departmentCode, startTime, endTime);
        }
        GroupRatingStatRespVO resp = groupRatingStatService.getGroupTagRating(type, departmentCode, startTime, endTime);
        return R.success(resp);
    }

    /**
     * 获取协同岗评分
     *
     * @param type 评分类型（1-直接评分，2-成员评分）
     * @param departmentCode 部门编码（可选）
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd）
     * @param peerId 目标节点 peerId，传入则查询对端节点数据
     * @return 评分统计结果
     */
    @GetMapping("/group/rating/coopUser")
    @Operation(summary = "获取协同岗评分", description = "根据时间范围和部门筛选，按协同岗统计各维度的平均评分")
    public R<GroupRatingStatRespVO> getCoopUserRating(
            @Parameter(description = "评分类型（1-直接评分，2-成员评分）")
            @RequestParam Integer type,
            @Parameter(description = "部门编码（可选）")
            @RequestParam(required = false) String departmentCode,
            @Parameter(description = "开始时间（可选，格式：yyyy-MM-dd）")
            @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（可选，格式：yyyy-MM-dd）")
            @RequestParam(required = false) String endTime,
            @Parameter(description = "目标节点 peerId，传入则查询对端节点数据")
            @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchRating(peerId, "/dashboard/v1/group/rating/coopUser", type, departmentCode, startTime, endTime);
        }
        GroupRatingStatRespVO resp = groupRatingStatService.getCoopUserRating(type, departmentCode, startTime, endTime);
        return R.success(resp);
    }

    /**
     * 通用：跨节点查询 rating 接口（统一参数：type/departmentCode/startTime/endTime）
     */
    private R dispatchRating(String peerId, String originUri, Integer type,
                             String departmentCode, String startTime, String endTime) {
        Map<String, String> params = new HashMap<>(4);
        if (type != null) params.put("type", String.valueOf(type));
        if (departmentCode != null) params.put("departmentCode", departmentCode);
        if (startTime != null) params.put("startTime", startTime);
        if (endTime != null) params.put("endTime", endTime);
        return nodeDispatchClient.dispatchAndParse(peerId, originUri, params, R.class);
    }
}
