package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/8/12
 **/
@Data
public class CollaborationClientVO {
    /**
     * id
     */
    private Long id;
    /**
     * 应用id
     */
    private String clientId;
    /**
     * 应用密钥
     */
    private String clientSecret;
    /**
     * 应用名称
     */
    private String clientName;
    /**
     * 应用类型
     */
    private String clientType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;
    /**
     * token有效期
     */
    private Integer tokenTime;
    /**
     * 刷新token有效期
     */
    private Integer refreshTokenTime;

    private Integer pageNum=1;

    private Integer pageSize=10;
}
