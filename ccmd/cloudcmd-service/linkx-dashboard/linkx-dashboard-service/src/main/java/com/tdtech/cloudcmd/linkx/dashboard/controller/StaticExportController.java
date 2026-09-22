package com.tdtech.cloudcmd.linkx.dashboard.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.linkx.dashboard.service.IStaticExportService;
import com.tdtech.cloudcmd.util.nodedispatch.DispatchedFile;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dashboard/v1")
@RequiredArgsConstructor
@Tag(name = "协同监测统计导出", description = "协同监测统计明细表导出功能")
public class StaticExportController {

    private final IStaticExportService staticExportService;

    @Autowired
    private NodeDispatchClient nodeDispatchClient;

    @GetMapping("/static/export")
    @Operation(summary = "导出协同监测统计明细表", description = "按部门编码与时间范围筛选，同步导出1个Excel文件，内含5个sheet")
    public void export(
            @Parameter(description = "部门编码（可选）")
            @RequestParam(required = false) String departmentCode,
            @Parameter(description = "开始时间（必填，格式：yyyy-MM-dd）")
            @RequestParam String startTime,
            @Parameter(description = "结束时间（必填，格式：yyyy-MM-dd）")
            @RequestParam String endTime,
            @Parameter(description = "是否包含子部门（0-不包含 / 1-包含，默认1）")
            @RequestParam(required = false) Integer includeChildren,
            @Parameter(description = "目标节点 peerId，传入则查询对端节点组织树") @RequestParam(required = false, value = "peerId") String peerId,
            HttpServletResponse response) throws Exception {
        log.info("导出协同监测明细入参: departmentCode={}, startTime={}, endTime={}, includeChildren={}, peerId={}",
                departmentCode, startTime, endTime, includeChildren, peerId);
        if (StringUtils.isNotBlank(peerId)) {
            // 跨节点查询：通过 linkx-node dispatch 透传到对端
            Map<String, String> params = new HashMap<>(2);
            if (departmentCode != null) params.put("departmentCode", departmentCode);
            if (startTime != null) params.put("startTime", startTime);
            if (endTime != null) params.put("endTime", endTime);
            if (includeChildren != null) params.put("includeChildren", includeChildren.toString());
            DispatchedFile file = nodeDispatchClient.dispatchAndDownload(
                    peerId, "/dashboard/v1/static/export", params,
                    response.getOutputStream(),
                    headers -> {
                        // 流式写开始前，透传对端的下载相关响应头
                        String ct = headers.getFirst(HttpHeaders.CONTENT_TYPE);
                        if (ct != null) response.setContentType(ct);
                        String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
                        if (cd != null) response.setHeader(HttpHeaders.CONTENT_DISPOSITION, cd);
                    });
            if (!file.isSuccess()) {
                // 失败：output 未写入任何字节，可自由返回错误响应
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":502,\"msg\":\"" + file.getErrorMessage() + "\"}");
            }
        } else {
            staticExportService.export(departmentCode, startTime, endTime, includeChildren, response);
        }
    }
}