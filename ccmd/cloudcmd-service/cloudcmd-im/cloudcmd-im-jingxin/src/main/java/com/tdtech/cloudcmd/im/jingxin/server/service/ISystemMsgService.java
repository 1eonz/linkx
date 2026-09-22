
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SystemMsg;

import java.util.Date;

public interface ISystemMsgService extends IService<SystemMsg> {

    Long count(Integer notifyType, Long bizId);

    Long count(Integer notifyType, Long bizId, Date startTime, Date endTime);

}