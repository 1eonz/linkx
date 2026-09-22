package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * 第三方登录，缓存实体类
 */
@Data
public class ThirdLoginCacheDto implements Serializable {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 应用id
     */
    private String clientId;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * userCode
     */
    private String openId;
    /**
     * 组织id
     */
    private String organization;
    /**
     * 角色类型
     */
    private String roleType;

}
