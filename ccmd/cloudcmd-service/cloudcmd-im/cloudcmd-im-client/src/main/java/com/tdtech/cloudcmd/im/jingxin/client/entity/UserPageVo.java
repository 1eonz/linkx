package com.tdtech.cloudcmd.im.jingxin.client.entity;

/**
 * @author lsc
 * @date 2025/7/14
 **/

import lombok.Data;

import java.util.List;

@Data
public class UserPageVo {

    private UniPage uniPage; // 分页信息 (表1-332)
    private List<UserListVo> records; // 人员列表 (表1-333)
}
