package com.tdtech.cloudcmd.linkx.third.vo;

import lombok.Data;

/**
 * 南向应用任务标准件派发配置
 */
@Data
public class AppCallableTaskConfigVo {
    /**
     * 是否开启任务标准件派发。0：否（默认）；1：是
     */
    private Integer enableTask;

    /**
     * 任务标准件派发时的自动表单回填配置，JSON 串
     */
    private String taskAutoFillConfig;
}