package com.tdtech.cloudcmd.auth.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.UserLog;
import com.tdtech.cloudcmd.auth.enums.UserLogEnums;
import com.tdtech.cloudcmd.auth.mapper.UserLogMapper;
import com.tdtech.cloudcmd.auth.service.IUserLogService;
import com.tdtech.cloudcmd.enums.MsgCodeEnum;
import com.tdtech.cloudcmd.util.IdWorker;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 用户操作日志记录表（目前只记录登陆日志。安全审计使用） 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
@Slf4j
public class UserLogServiceImpl extends ServiceImpl<UserLogMapper, UserLog> implements IUserLogService {

    @Autowired
    private IdWorker idWorker;

    @Override
    public void writeToUserLog(Long userId, String remoteIp, UserLogEnums operationType) {
        Date date = new Date();
        UserLog userLog = UserLog.builder().id(idWorker.nextId()).userId(userId).operationTime(date)
            .operationType(operationType.getCode()).content(operationType.getMsg())
            .result(MsgCodeEnum.SUCCESS.getCode()).ip(remoteIp).gmtCreated(date).gmtModified(date).build();
        this.save(userLog);
    }
}
