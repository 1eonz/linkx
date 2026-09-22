package com.tdtech.cloudcmd.linkx.third.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.linkx.third.dto.ChannelMessageDto;
import com.tdtech.cloudcmd.linkx.third.entity.PushMsg;

public interface PushMsgService extends IService<PushMsg> {


    Boolean recordMessage(ChannelMessageDto channelMessageDto);

    Page<ChannelMessageDto> messageRecordPage(Long userId, String keywords, Integer page, Integer pageSize, Long start, Long end);
}
