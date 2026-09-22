package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostLog;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ly
 * @date 2025/10/15
 **/
@Tag(name = "协同岗操作记录")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/post/log")
@RequiredArgsConstructor
public class CollaborationPostLogController {

    private static final String EXPORT_FILE_NAME = "协同岗操作记录";

    @Resource
    private CollaborationPostLogService collaborationPostLogService;

    @Operation(summary = "分页查询操作记录", description = "分页查询协同岗操作记录")
    @GetMapping("/page")
    public R<Page<CollaborationPostLog>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String postName,
                                              @RequestParam(required = false) Long orgId,
                                              @RequestParam(required = false) String orgName,
                                              @RequestParam(required = false) String relatedUserNames,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime) {
        return R.success(collaborationPostLogService.page(pageNum, pageSize, postName, orgId, orgName, relatedUserNames, startTime, endTime));
    }

    @Operation(summary = "导出记录", description = "导出协同岗操作记录")
    @GetMapping("/export")
    public void exportToExcel(@RequestParam(required = false) String postName,
                              @RequestParam(required = false) Long orgId,
                              @RequestParam(required = false) String orgName,
                              @RequestParam(required = false) String relatedUserNames,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              HttpServletResponse response)
            throws IOException {
        collaborationPostLogService.exportToExcel(EXPORT_FILE_NAME, postName, orgId, orgName, relatedUserNames, startTime, endTime, response);
    }

}
