package com.tdtech.cloudcmd.linkx.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 客户端登录信息记录表导出记录
 */
@Data
@Accessors(chain = false)
@NoArgsConstructor
@AllArgsConstructor
public class StatisticLoginVO implements Serializable {

    /**
     * 警信用户ID
     */
    private String userId;

    /**
     * AppId
     */
    private String appId;

    /**
     * 客户端类型(1:BS PC;2:CS PC;3:App H5;4:Admin;5:RESTful;6:JS-SDK)
     */
    private String clientType;

    /**
     * 操作系统(如 Windows 10, iOS 15, Linux)
     */
    private String os;

    /**
     * 浏览器信息(Chrome 120/Safari 17，Server类型可为空)
     */
    private String browser;

    /**
     * 屏幕窗口物理长宽像素
     */
    private String screen;

    /**
     * 登录时间
     */
    private String loginTime;

    /**
     * 登录结果(0：成功；1：失败)
     */
    private String loginResult;
}
