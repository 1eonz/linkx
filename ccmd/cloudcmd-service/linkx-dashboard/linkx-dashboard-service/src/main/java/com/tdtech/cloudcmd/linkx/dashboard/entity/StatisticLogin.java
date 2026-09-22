package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端登录信息记录表
 * TableName：tb_statistic_login
 */
@TableName("tb_statistic_login")
@Data
@Accessors(chain = false)
@NoArgsConstructor
@AllArgsConstructor
public class StatisticLogin implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
    private Integer clientType;

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
    private Date loginTime;

    /**
     * 登录结果(0：成功；1：失败)
     */
    private Integer loginResult;
}
