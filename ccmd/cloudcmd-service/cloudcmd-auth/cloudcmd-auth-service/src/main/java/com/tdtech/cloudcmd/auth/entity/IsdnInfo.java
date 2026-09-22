package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;

import lombok.Data;

/***
 * 通讯账户用户名和密码
 */
@Data
public class IsdnInfo implements Serializable {
    // ISDN号返回给前端
    private String account;
    // ISDN密码返回给前端
    private String password;
}
