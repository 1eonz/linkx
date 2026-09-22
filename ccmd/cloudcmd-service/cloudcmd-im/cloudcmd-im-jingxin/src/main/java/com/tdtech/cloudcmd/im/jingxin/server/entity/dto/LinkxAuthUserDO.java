package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.mysql.encrypt.annotation.EncryptField;
import lombok.Data;

import java.util.Date;

/**
 * linkx_auth.tb_user_im 表实体
 * 用于 im-jingxin 模块将 IM 用户数据落库持久化（敏感字段加密存储）
 */
@Data
@TableName("`linkx_auth`.`tb_user_im`")
public class LinkxAuthUserDO {

    @TableId
    private Long id;

    private String code;

    @EncryptField
    private String name;

    private String avatar;
    private String gender;

    @EncryptField
    private String mobile;

    private String email;
    private String isdn;

    @EncryptField
    private String idCard;

    private String district;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long directLeaderId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String directLeaderName;

    private String departmentCode;
    private String departmentName;
    private Long departmentId;

    private Integer type;
    private Integer status;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String password;
    private Date pwdTime;

    private Date gmtCreated;
    private Date gmtUpdated;
}