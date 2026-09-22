package com.tdtech.cloudcmd.msip.constant;

/**
 * @author mWX556161
 * @date 2020/11/23 15:49
 */
public interface MSIPConstant {

    // 操作成功
    int OPERATION_SUCCESS = 0;

    // 操作失败
    int OPERATION_FAILURE = 1;

    // license缓存key
    String LICENSE_KEY = "msip:license";

    // license lock key
    String LICENSE_LOCK_KEY = "msip:license:lock";

    // 活动告警key
    String ALERT_ACTIVE_RECORD_KEY = "msip:alarm:active:record:%s";

    // 告警lock key
    String ALERT_LOCK_KEY = "msip:alarm:active:lock:%s";

    // 擦除告警lock key
    String CLEAR_ALERT_LOCK_KEY = "msip:alarm:clear:lock:%s";

    // 功能不可用
    String NOT_AVAILABLE = "0";

    // 功能可用
    String AVAILABLE = "1";

    // 最大并发数
    Integer MAX_LIMIT_COUNT = 500;
}
