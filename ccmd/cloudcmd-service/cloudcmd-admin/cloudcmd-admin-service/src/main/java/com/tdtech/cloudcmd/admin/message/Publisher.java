package com.tdtech.cloudcmd.admin.message;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * 发布事件的接口
 *
 * @author : mWX556161
 * @date : 2020-05-15 10:12
 */
public interface Publisher {

    String ADMIN_TO_CAGENT = "adminTOCagent";

    String GLOBAL_CHANGED = "cloudcmd-base-global";

    String ICP_CONFIG_CHANGED = "cloudcmd-icp-config";

    @Output(ADMIN_TO_CAGENT)
    MessageChannel globalOutPut();

    @Output(GLOBAL_CHANGED)
    MessageChannel globalConfigChange();

    @Output(ICP_CONFIG_CHANGED)
    MessageChannel icpConfigChange();

}
