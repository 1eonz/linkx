package com.tdtech.cloudcmd.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户管理员表
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("linkx_auth.tb_user_admin")
public class UserAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 应用ID。引用自linkx_auth.tb_application.id
     */
    private Long applicationId;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 用户账号，用于登录
     */
    private String account;

    /**
     * 关联警信用户ID
     */
    private Long imUserId;

    /**
     * 关联警信用户名称
     */
    @TableField(exist = false)
    private String imUserName;

    /**
     * 关联的警信用户所属组织部门ID
     */
    private Long imUserDeptId;

    /**
     * salt+用户密码
     */
    private String password;

    private Long createUserId;

    /**
     * 最后一次登录的UTC时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 状态：0-表示可用，1-表示禁用  2-锁定（冻结）
     */
    private Integer status;

    /**
     * 是否首次登录。默认：1（是）；0（否）
     */
    private Integer firstLogin;

    /**
     * 连续密码错误计数，登录成功后重置为0。
     */
    private Integer errorPwdCount;

    /**
     * 最后一次密码错误的UTC时间
     */
    private LocalDateTime errorPwdLastTime;

    /**
     * 最后修改时间
     */
    private LocalDateTime gmtModified;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;

}
