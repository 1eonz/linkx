package com.tdtech.cloudcmd.msip.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MSIPLoginRequest {
    // 账号
    private String username;

    // 密码
    private String password;

    public MSIPLoginRequest() {
        super();
    }

    public MSIPLoginRequest(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

}
