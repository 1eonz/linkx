package com.tdtech.cloudcmd.linkx.dashboard.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.linkx.dashboard.entity.GroupMsgSendStatic;
import com.tdtech.cloudcmd.linkx.dashboard.service.IGroupMsgSendStaticService;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author S063874
 * @date 2026/3/17
 **/
@Slf4j
@RestController
@RequestMapping("/dashboard/v1")
@RequiredArgsConstructor
@Tag(name = "群组消息统计", description = "群组消息统计（不含消息内容）")
public class GroupMsgStaticController {

    private final IGroupMsgSendStaticService groupMsgSendStaticService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;

    /**
     * 新增群组消息统计
     *
     * @param groupMsgSendStatic 群组消息统计信息
     * @return 消息ID
     */
    @PostMapping("/group/msg/static")
    @Operation(summary = "新增群组消息统计", description = "新增单条群组消息统计记录")
    public R<Long> createGroupMsgSendStatic(@Valid @RequestBody GroupMsgSendStatic groupMsgSendStatic) {
        Long msgId = groupMsgSendStaticService.createGroupMsgSendStatic(groupMsgSendStatic);
        return R.success(msgId);
    }

    /**
     * 批量新增群组消息统计
     *
     * @param groupMsgSendStaticList 群组消息统计列表
     * @return 成功数量
     */
    @PostMapping("/group/msg/static/batch")
    @Operation(summary = "批量新增群组消息统计", description = "批量新增群组消息统计记录")
    public R<Integer> batchCreateGroupMsgSendStatic(@Valid @RequestBody List<GroupMsgSendStatic> groupMsgSendStaticList) {
        int count = groupMsgSendStaticService.batchCreateGroupMsgSendStatic(groupMsgSendStaticList);
        return R.success(count);
    }

    /**
     * 批量新增群组消息统计（过滤已存在的消息ID）
     *
     * @param groupMsgSendStaticList 群组消息统计列表
     * @return 成功数量
     */
    @PostMapping("/group/msg/static/batch/filter")
    @Operation(summary = "批量新增群组消息统计（过滤已存在）", description = "批量新增群组消息统计记录，自动过滤已存在的消息ID")
    public R<Integer> batchCreateGroupMsgSendStaticWithFilter(@Valid @RequestBody List<GroupMsgSendStatic> groupMsgSendStaticList) {
        int count = groupMsgSendStaticService.batchCreateGroupMsgSendStaticWithFilter(groupMsgSendStaticList);
        return R.success(count);
    }

    /**
     * 获取群组消息统计
     * @param departmentCode
     * @param startTime
     * @param endTime
     * @param topn
     * @return
     */
    @GetMapping("/group/msgs/top")
    public R getGroupMsgTop(@RequestParam(required = false) String departmentCode,
                            @RequestParam(required = false) String startTime,
                            @RequestParam(required = false) String endTime,
                            @RequestParam(required = false) Integer topn){
        return R.success(groupMsgSendStaticService.countTopMessageByUserId(departmentCode, startTime, endTime, topn));
    }
}
