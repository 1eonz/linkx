package com.tdtech.cloudcmd.linkx.third.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.linkx.third.dto.ChannelMessageDto;
import com.tdtech.cloudcmd.linkx.third.service.impl.PushMsgServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "应用分组管理", description = "应用分组相关接口")
@RestController
@RequestMapping("/third/v1/push-message")
public class MessageRecordController {

    @Resource
    private PushMsgServiceImpl pushMsgService;

    @PostMapping
    public R<Boolean> recordMessage(@RequestBody ChannelMessageDto channelMessageDto) {
        return R.success(pushMsgService.recordMessage(channelMessageDto));
    }

    @GetMapping
    public R<Page<ChannelMessageDto>> getMessageRecord(@RequestParam(value = "userId", required = false) Long userId,
                                                       @RequestParam(value = "keywords", required = false) String keywords,
                                                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(value = "start", required = false) Long start,
                                                       @RequestParam(value = "end", required = false) Long end) {
        return R.success(pushMsgService.messageRecordPage(userId, keywords, page, pageSize, start, end));
    }
}
