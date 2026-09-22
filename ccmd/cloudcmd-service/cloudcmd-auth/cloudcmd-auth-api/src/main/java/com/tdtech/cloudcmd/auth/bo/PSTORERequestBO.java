package com.tdtech.cloudcmd.auth.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author michstabe
 * @date 2023/6/12 15:25
 * @description 三方校验请求实体类
 */

@Data
public class PSTORERequestBO implements Serializable {

    public static final String url = "%s/pstore/service/user/userInfo";

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 应用密钥
     */
    private String clientId;
}
