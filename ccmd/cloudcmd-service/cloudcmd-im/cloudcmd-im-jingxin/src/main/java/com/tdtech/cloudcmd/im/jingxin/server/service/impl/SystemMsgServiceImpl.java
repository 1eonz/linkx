package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SystemMsg;
import com.tdtech.cloudcmd.im.jingxin.server.service.ISystemMsgService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.SystemMsgMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class SystemMsgServiceImpl extends ServiceImpl<SystemMsgMapper, SystemMsg> implements ISystemMsgService {

    @Resource
    private SystemMsgMapper systemMsgMapper;

    @Override
    public Long count(Integer notifyType, Long bizId) {
        LambdaQueryWrapper<SystemMsg> queryWrapper = new LambdaQueryWrapper<SystemMsg>()
                .eq(SystemMsg::getNotifyType, notifyType)
                .eq(SystemMsg::getBizId, bizId);
        return count(queryWrapper);
    }

    @Override
    public Long count(Integer notifyType, Long bizId, Date startTime, Date endTime) {
        LambdaQueryWrapper<SystemMsg> queryWrapper = new LambdaQueryWrapper<SystemMsg>()
                .eq(SystemMsg::getNotifyType, notifyType)
                .eq(SystemMsg::getBizId, bizId)
                .ge(SystemMsg::getGmtCreated, startTime)
                .le(SystemMsg::getGmtCreated, endTime);
        return count(queryWrapper);
    }
}