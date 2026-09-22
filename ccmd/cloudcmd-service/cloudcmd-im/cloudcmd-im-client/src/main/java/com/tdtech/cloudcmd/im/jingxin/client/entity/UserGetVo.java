package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/19
 **/
@Data
public class UserGetVo {

    private List<ImUser> results;
    private List<UserFailVo> failures;
}
