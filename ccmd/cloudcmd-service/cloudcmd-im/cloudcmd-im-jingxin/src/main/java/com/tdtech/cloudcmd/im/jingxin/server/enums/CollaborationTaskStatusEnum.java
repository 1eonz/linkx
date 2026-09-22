package com.tdtech.cloudcmd.im.jingxin.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 协作任务状态
 */
@AllArgsConstructor
@Getter
public enum CollaborationTaskStatusEnum {
    PENDING(1, "待办"),
    TRACKING(2, "跟踪"),
    FINISHED(3, "已办结"),
    NO_NEED_REPLY(4, "无需回复"),
    DELAY(7, "未及时回复"),
    OVERDUE(8, "已逾期");

    private final int value;
    private final String desc;

    public static CollaborationTaskStatusEnum of(Integer status) {
        if (status == null) {
            return null;
        }
        for (CollaborationTaskStatusEnum s : values()) {
            if (s.value == status) {
                return s;
            }
        }
        return null;
    }
}