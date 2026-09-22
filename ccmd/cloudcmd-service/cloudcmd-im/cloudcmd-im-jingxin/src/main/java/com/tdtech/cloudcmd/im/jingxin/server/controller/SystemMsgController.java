package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.im.jingxin.server.service.ISystemMsgService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "消息通知")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/system/msg")
@RequiredArgsConstructor
@Validated
public class SystemMsgController {

    @Resource
    private ISystemMsgService systemMsgService;


}
