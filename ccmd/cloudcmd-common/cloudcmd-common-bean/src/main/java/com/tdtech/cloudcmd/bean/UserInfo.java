package com.tdtech.cloudcmd.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tdtech.cloudcmd.util.json.JsonArray;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author zhuangzl
 * @date 2020-06-01 17:31
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfo implements Serializable {

    private Long userId;
    @JsonProperty("clientId")
    private String appKey;
    /**
     * 设备唯一标识
     */
    private String deviceId;
    /**
     * 用户名（默认使用警员编号）
     */
    private String userName;

    private String executorCode;
    /**
     * 缓存创建时间
     */
    private Date createTime;
    /**
     * 登录Ip
     */
    private String loginIp;

    /**
     * 用户有效期
     */
    private Date userPeriod;

    @Deprecated
    private Long executorId;

    private boolean isAdmin;

    private Long organizationId;
    private String organizationCode;

    @Deprecated
    private Integer hasChildOrgPriv;

    private JsonArray imOrgPrivs;

    /**
     * 数据权限-组织id
     */
    private List<Long> imOrgPrivIds;

    /**
     * 数据权限-组织code
     */
    private List<String> imOrgPrivCodes;

    /**
     * 角色是否无数据权限
     */
    private Boolean isRoleNoAuth = false;

    private String idCardNum;
}
