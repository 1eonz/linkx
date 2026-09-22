package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.entity.RoleDto;
import com.tdtech.cloudcmd.util.json.JsonArray;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author mWX556161
 * @date 2020/11/20 16:39
 */
@Data
public class OAuthLoginDto implements Serializable {

    private String accessToken;

    private int expireIn;

    private String refreshToken;

    private int refreshTokenExpireIn;

    private String scope;

    private String tokenType;

    private String userId;

    /**
     * 新增isdn编号
     */
    private String isdncode;

    /**
     * 新增isdn密码
     */
    private String isdnpass;

    /**
     * 针对capp返回身份
     */
    private int memberType;

    /**
     * 用户名
     */
    private String userName;

    private String account;

    /**
     * 是否协同岗
     */
    @Deprecated
    private Boolean collaboration;

    private String idCardNum;

    private JsonArray imOrgPrivs;

    private Boolean isAdmin;

    private List<RoleDto> roles;

    /**
     * 角色是否无数据权限
     */
    private Boolean isRoleNoAuth = false;
}
