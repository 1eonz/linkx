package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/24 17:03
 */

@Data
public class OAuthLoginCacheDto implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 应用ID
     */
    private String appKey;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 缓存创建时间
     */
    private String createTime;

    /**
     * 登录Ip
     */
    private String loginIp;

    /**
     * 用户有效期
     */
    private Date userPeriod;

}
