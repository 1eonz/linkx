package com.tdtech.cloudcmd.auth.dto;

import com.tdtech.cloudcmd.auth.entity.DepartmentUserCustom;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 自定义通讯录用户视图对象。
 */
@Schema(description = "自定义通讯录用户视图对象")
public class DepartmentCustomUserVO extends DepartmentUserCustom {

    @Schema(description = "警信用户信息")
    private ImUserDO user;

    public ImUserDO getUser() {
        return user;
    }

    public void setUser(ImUserDO user) {
        this.user = user;
    }
}
