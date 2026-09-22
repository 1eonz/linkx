package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.linkx.third.dto.ChannelMessageDto;
import com.tdtech.cloudcmd.linkx.third.entity.PushMsg;
import com.tdtech.cloudcmd.linkx.third.mapper.PushMsgMapper;
import com.tdtech.cloudcmd.linkx.third.service.PushMsgService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class PushMsgServiceImpl extends ServiceImpl<PushMsgMapper, PushMsg> implements PushMsgService {

    @Resource
    private IdWorker idWorker;

    @Override
    public Boolean recordMessage(ChannelMessageDto channelMessageDto) {
        if (channelMessageDto.getReceivedUserId() == null) {
            var user = SecurityUtils.getUser();
            if (user == null) {
                throw new BusinessException("用户未登录");
            }
            channelMessageDto.setReceivedUserId(user.getUserId());
        }

        PushMsg msg = BeanCopyUtils.copyBean(channelMessageDto, PushMsg::new);
        msg.setId(idWorker.nextId());
        var now = new Date();
        msg.setGmtCreateTime(now);
        msg.setGmtLastModified(now);
        msg.setReceivedAt(now);
        baseMapper.insert(msg);
        return true;
    }

    @Override
    public Page<ChannelMessageDto> messageRecordPage(Long userId, String keywords, Integer page, Integer pageSize, Long start, Long end) {
        if (userId == null) {
            var user = SecurityUtils.getUser();
            if (user == null) {
                throw new BusinessException("用户未登录");
            }

            userId = user.getUserId();
        }

        Page<PushMsg> msgPage = baseMapper.messageRecordPage(userId, keywords, start, end, Page.of(page, pageSize));

        Page<ChannelMessageDto> pageResult = BeanCopyUtils.copyBean(msgPage, Page::new);
        pageResult.setRecords(BeanCopyUtils.copyList(msgPage.getRecords(), ChannelMessageDto::new));
        return pageResult;
    }
}
