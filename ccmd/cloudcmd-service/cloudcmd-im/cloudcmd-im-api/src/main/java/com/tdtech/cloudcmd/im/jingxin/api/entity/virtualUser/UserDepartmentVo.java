package com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDepartmentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String shortName;

    private Long parentId;

    private Integer sort;

    private Boolean isPrimary;

}