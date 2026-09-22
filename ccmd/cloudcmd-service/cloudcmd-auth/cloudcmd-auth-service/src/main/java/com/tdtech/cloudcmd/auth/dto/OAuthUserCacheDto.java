package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import com.tdtech.cloudcmd.util.json.JsonArray;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/24 16:44
 */
@Data
public class OAuthUserCacheDto implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 执行者ID
     */
    private Long executorId;

    /**
     * 组织ID
     */
    private Long organizationId;

    /**
     * 角色。可选。
     */
    private String role;

    private String accessToken;

    private String refreshToken;

    private JsonArray imOrgPrivs;
}
