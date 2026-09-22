package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/12/16 17:48
 */
@Data
public class TokenRefreshMsg implements Serializable {

    private String oldToken;

    private String newToken;
}
