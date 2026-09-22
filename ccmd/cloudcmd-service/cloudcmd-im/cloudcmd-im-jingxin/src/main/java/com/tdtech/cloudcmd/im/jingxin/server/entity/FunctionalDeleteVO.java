package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * 部门协同岗用户关联实体类
 */
@Data
public class FunctionalDeleteVO{



    private Long id;

    private String departmentName;

    private String postName;
}