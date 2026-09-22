package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostOnlineVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationDispositionVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PersonnelVerificationVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationStatisticsService;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import com.tdtech.linkx.node.api.PeerNodeRpcApi;
import com.tdtech.linkx.node.util.PeerNodeIconUrlUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Tag(name = "协同统计")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/statistics")
public class CollaborationStatisticsController {

    @Resource
    private CollaborationStatisticsService collaborationStatisticsService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;
    @Resource
    private PeerNodeRpcApi peerNodeRpcApi;

    /**
     * 协同岗统计
     */
    @GetMapping("/count")
    public R count(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/count", departmentCode, startTime, endTime);
        }
        CollaborationStatisticsVO count = collaborationStatisticsService.count(departmentCode, startTime, endTime);
        return R.success(count);
    }

    /**
     * 获取所有任务的状态统计
     */
    @GetMapping("/task/count")
    public R taskCount(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/task/count", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.calTaskCount(departmentCode, startTime, endTime));
    }

    /**
     * 获取零在岗人员协同岗列表
     */
    @GetMapping("/listZeroOnDutyPosts")
    public R listZeroOnDutyPosts(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/listZeroOnDutyPosts", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.listZeroOnDutyPosts(departmentCode, startTime, endTime));
    }

    /**
     * 回复统计top10
     */
    @GetMapping("/replyCount")
    public R replyCount(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/replyCount", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.replyCount(departmentCode, startTime, endTime));
    }

    /**
     * 平均时长回复统计
     */
    @GetMapping("/replyDuration")
    public R replyDuration(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/replyDuration", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.replyDuration(departmentCode, startTime, endTime));
    }

    /**
     * 协同处置消息top10
     */
    @GetMapping("/dispositionCount")
    public R dispositionCount(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            R remoteResp = dispatchStats(peerId, "/collaboration/v1/statistics/dispositionCount", departmentCode, startTime, endTime);
            String baseUrl = resolvePeerBaseUrl(peerId);
            return rewriteIconUrl(baseUrl, remoteResp,
                    new TypeReference<List<CollaborationDispositionVO>>() {},
                    (vo, b) -> vo.setIconUrl(PeerNodeIconUrlUtil.prepend(b, vo.getIconUrl())));
        }
        return R.success(collaborationStatisticsService.dispositionCount(departmentCode, startTime, endTime));
    }

    /**
     * 统计协同岗平均回复时长top10
     */
    @GetMapping("/disposition/replyDuration")
    public R dispositionReplyDuration(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            R remoteResp = dispatchStats(peerId, "/collaboration/v1/statistics/disposition/replyDuration", departmentCode, startTime, endTime);
            String baseUrl = resolvePeerBaseUrl(peerId);
            return rewriteIconUrl(baseUrl, remoteResp,
                    new TypeReference<List<CollaborationDispositionVO>>() {},
                    (vo, b) -> vo.setIconUrl(PeerNodeIconUrlUtil.prepend(b, vo.getIconUrl())));
        }
        return R.success(collaborationStatisticsService.dispositionReplyDuration(departmentCode, startTime, endTime));
    }

    /**
     * 创群榜单根据组织统计
     */
    @GetMapping("/group/create/count")
    public R groupCreateCount(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/group/create/count", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.groupCreateCountByCode(departmentCode, startTime, endTime, null));
    }

    /**
     * 根据人员统计创群个数
     */
    @GetMapping("/group/create/count/by/user")
    public R groupCreateCountByUser(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/group/create/count/by/user", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.groupCreateCountByUser(departmentCode, startTime, endTime));
    }

    /**
     * 协同岗在线统计列表
     */
    @GetMapping("/online/list")
    public R<List<CollaborationPostOnlineVO>> onlineStatistics(@RequestParam(required = false) String departmentCode,
        @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
        @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            R remoteResp = dispatchStats(peerId, "/collaboration/v1/statistics/online/list", departmentCode, startTime, endTime);
            String baseUrl = resolvePeerBaseUrl(peerId);
            return rewriteIconUrl(baseUrl, remoteResp,
                    new TypeReference<List<CollaborationPostOnlineVO>>() {},
                    (vo, b) -> vo.setIconUrl(PeerNodeIconUrlUtil.prepend(b, vo.getIconUrl())));
        }
        return R.success(collaborationStatisticsService.onlineStatistics(departmentCode, startTime, endTime));
    }

    private <T> R<List<T>> rewriteIconUrl(String baseUrl, R<?> remoteResp, TypeReference<List<T>> typeRef,
                                          BiConsumer<T, String> iconUrlSetter) {
        if (remoteResp == null || remoteResp.getData() == null) {
            return R.success((List<T>) null);
        }
        Object data = remoteResp.getData();
        if (!(data instanceof List)) {
            // 保留对端错误码/msg，仅丢弃非 List 数据，避免类型不一致
            return R.success(remoteResp.getCode(), remoteResp.getMsg(), null);
        }
        List<T> posts = JsonUtil.parseJson(JsonUtil.toJsonStr(data), typeRef);
        if (baseUrl != null) {
            for (T vo : posts) {
                iconUrlSetter.accept(vo, baseUrl);
            }
        }
        return R.success(posts);
    }

    private String resolvePeerBaseUrl(String peerId) {
        try {
            Map<String, PeerNodeRpcApi.PeerNodeInfo> infoMap =
                    peerNodeRpcApi.getPeerInfoMap(Collections.singletonList(peerId));
            if (infoMap != null && infoMap.containsKey(peerId)) {
                return PeerNodeIconUrlUtil.buildBaseUrl(infoMap.get(peerId).getIp());
            }
        } catch (Exception e) {
            log.warn("resolvePeerBaseUrl: getPeerInfoMap failed, peerId={}", peerId, e);
        }
        return null;
    }

    /**
     * 人员核查统计数量
     */
    @GetMapping("/personnel/verification/list")
    public R<List<PersonnelVerificationVO>> personnelVerification(
            @RequestParam(required = false) String departmentCode,
            @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            return dispatchStats(peerId, "/collaboration/v1/statistics/personnel/verification/list", departmentCode, startTime, endTime);
        }
        return R.success(collaborationStatisticsService.personnelVerification(departmentCode, startTime, endTime));
    }

    /**
     * 通用：跨节点查询 statistics 接口（统一参数：departmentCode/startTime/endTime）
     */
    private R dispatchStats(String peerId, String originUri, String departmentCode, String startTime, String endTime) {
        Map<String, String> params = new HashMap<>(3);
        if (departmentCode != null) params.put("departmentCode", departmentCode);
        if (startTime != null) params.put("startTime", startTime);
        if (endTime != null) params.put("endTime", endTime);
        return nodeDispatchClient.dispatchAndParse(peerId, originUri, params, R.class);
    }
}
