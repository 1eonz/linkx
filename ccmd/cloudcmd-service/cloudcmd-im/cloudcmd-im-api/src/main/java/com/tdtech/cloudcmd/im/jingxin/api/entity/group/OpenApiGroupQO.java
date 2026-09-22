package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import lombok.Data;

import java.io.Serializable;

@Data
public class OpenApiGroupQO implements Serializable {

    private Long userId;

    /**
     * 0：全部；1：普通群组；2：协同群组
     */
    private Integer groupType = 2;

    /**
     * 0：全部；1：一键建群（default）；5：一键调度；4：职能建群；3：自定义建群
     */
    private Integer createType = 1;

    /**
     * 1:我创建的；3：我可查看的但不是成员；4：我是成员；5：我的所有（default）
     */
    private Integer scope = 5;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
