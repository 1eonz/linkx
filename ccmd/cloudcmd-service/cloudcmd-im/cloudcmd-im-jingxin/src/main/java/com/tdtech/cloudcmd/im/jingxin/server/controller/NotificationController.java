package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "任务通知接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/notification")
public class NotificationController {

    @Resource
    private INotificationService notificationService;

    @Operation(summary = "任务通知已读更新", description = "将指定通知标记为已读")
    @PostMapping("/read")
    public R<Integer> markRead(
            @Parameter(description = "通知ID", required = true) @RequestBody Map<String, Long> body) {
        Long notificationId = body.get("notificationId");
        int count = notificationService.markRead(notificationId);
        return R.success(count);
    }

    @Operation(summary = "任务通知模块列表", description = "返回当前用户有协作消息的模块名称列表（collaborative_msg=1）")
    @GetMapping("/modules")
    public R<List<String>> listModules() {
        List<String> modules = notificationService.listModuleNames();
        return R.success(modules);
    }

    @Operation(summary = "任务通知计数", description = "返回指定模块下当前用户的已读、未读通知计数")
    @GetMapping("/modules/{moduleName}/count")
    public R<Map<String, Integer>> countByModule(
            @Parameter(description = "模块名称", required = true) @PathVariable String moduleName) {
        int unread = notificationService.countUnread(moduleName);
        int read = notificationService.countRead(moduleName);
        Map<String, Integer> result = new HashMap<>();
        result.put("unread", unread);
        result.put("read", read);
        result.put("total", unread + read);
        return R.success(result);
    }
}
