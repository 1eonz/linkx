package com.tdtech.cloudcmd.linkx.dashboard.controller;

import com.tdtech.cloudcmd.linkx.dashboard.service.IStatisticLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * LINKX增加日活数据统计
 */
@Slf4j
@RestController
@RequestMapping("/dashboard/v1")
@Tag(name = "LINKX增加日活数据统计", description = "LINKX增加日活数据统计")
public class StatisticLoginController {
    @Resource
    private IStatisticLoginService statisticLoginService;

    /**
     * 日活数据导出
     *
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     */
    @GetMapping("/statistic/login/export")
    @Operation(summary = "日活数据导出", description = "根据时间范围筛选，日活数据导出")
    public void export(
            @Parameter(description = "开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) String endTime, HttpServletResponse response) throws IOException {
        statisticLoginService.export(startTime, endTime, response);
    }
}
