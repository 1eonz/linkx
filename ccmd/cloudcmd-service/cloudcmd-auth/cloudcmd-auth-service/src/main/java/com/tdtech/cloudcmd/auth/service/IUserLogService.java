package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.entity.UserLog;
import com.tdtech.cloudcmd.auth.enums.UserLogEnums;

/**
 * <p>
 * 用户操作日志记录表（目前只记录登陆日志。安全审计使用） 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
public interface IUserLogService extends IService<UserLog> {

    /**
     * 写入用户操作日志
     * 
     * @param userId
     * @param remoteIp
     * @param operationType
     */
    void writeToUserLog(Long userId, String remoteIp, UserLogEnums operationType);
}
