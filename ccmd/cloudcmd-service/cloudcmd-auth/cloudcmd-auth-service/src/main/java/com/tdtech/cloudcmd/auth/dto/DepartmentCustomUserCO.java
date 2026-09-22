package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 自定义通讯录用户操作请求参数。
 */
@Schema(description = "自定义通讯录用户操作请求参数")
public class DepartmentCustomUserCO {

    @Schema(description = "用户ID列表", required = true, example = "[1001, 1002, 1003]")
    private List<ImUserDO> users;

    public List<ImUserDO> getUsers() {
        return users;
    }

    public void setUsers(List<ImUserDO> users) {
        this.users = users;
    }
}
