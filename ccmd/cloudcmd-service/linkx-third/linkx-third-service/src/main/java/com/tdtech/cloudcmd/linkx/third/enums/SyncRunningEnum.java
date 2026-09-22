package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncRunningEnum{
//    接口任务执行状态。0：未启动；1：运行中；2：正常结束；3：异常结束
    NOT_STARTED(0, "未启动"),
    RUNNING(1, "运行中"),
    SUCCESS(2, "正常结束"),
    ERROR(3, "异常结束");

    private final Integer code;

    private final String msg;
}
