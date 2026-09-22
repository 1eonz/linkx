package com.tdtech.cloudcmd.auth.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author michstabe
 * @date 2023/6/12 15:26
 * @description 三方校验用户实体类
 */

@Data
public class PSTOREUserBO implements Serializable {

    /**
     * 描述
     */
    private String description;

    /**
     * 用户名
     */
    private String username;

    /**
     * 身份证号
     */
    private String idcard;

    /**
     * 姓名
     */
    private String realname;

    /**
     * 电话
     */
    private String phone;

    /**
     * 部门机构编码
     */
    private String deptId;

    /**
     * 部门机构名称
     */
    private String deptName;

    /**
     * 登陆时间
     */
    private String login_time;
}
