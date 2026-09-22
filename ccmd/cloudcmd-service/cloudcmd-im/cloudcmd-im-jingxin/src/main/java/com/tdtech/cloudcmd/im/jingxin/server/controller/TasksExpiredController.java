package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpiredVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExpiredService;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "协同岗无人值守预警")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/tasks/expired")
@RequiredArgsConstructor
@Validated
public class TasksExpiredController {

    @Resource
    private ITasksExpiredService tasksExpiredService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;

    @Operation(summary = "分页查询任务逾期预警列表")
    @GetMapping("/page")
    public R<Page<TasksExpiredVO>> getPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keywords,
            @Parameter(description = "部门编码") @RequestParam(required = false) String departmentCode,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "目标节点 peerId，传入则查询对端节点数据") @RequestParam(required = false) String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            Map<String, String> params = new HashMap<>(6);
            params.put("pageNum", String.valueOf(pageNum));
            params.put("pageSize", String.valueOf(pageSize));
            if (keywords != null) params.put("keywords", keywords);
            if (departmentCode != null) params.put("departmentCode", departmentCode);
            if (startTime != null) params.put("startTime", startTime);
            if (endTime != null) params.put("endTime", endTime);
            return nodeDispatchClient.dispatchAndParse(
                    peerId, "/collaboration/v1/tasks/expired/page", params, R.class);
        }
        return R.success(tasksExpiredService.findPage(pageNum, pageSize, keywords, departmentCode, startTime, endTime));
    }

}
