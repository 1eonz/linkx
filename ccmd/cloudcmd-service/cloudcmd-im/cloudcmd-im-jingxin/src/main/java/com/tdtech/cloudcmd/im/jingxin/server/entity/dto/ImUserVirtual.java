package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 虚拟用户信息表
 */
@Data
@TableName(value = "`linkx_auth`.`tb_im_user_virtual`")
public class ImUserVirtual implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull(message = "主键ID不能为null")
    private Long id;

    /**
     * 虚拟用户名称（同警信后台设置的名称）
     */
    @TableField(value = "`user_name`")
    @Size(max = 100, message = "虚拟用户名称（同警信后台设置的名称）最大长度要小于 100")
    @NotBlank(message = "虚拟用户名称（同警信后台设置的名称）不能为空")
    private String userName;

    /**
     * 通讯号码（警信后台开户账号）
     */
    @TableField(value = "`contact_number`")
    @Size(max = 50, message = "通讯号码（警信后台开户账号）最大长度要小于 50")
    private String contactNumber;

    /**
     * 应用ID（警信后台开户获取）
     */
    @TableField(value = "`app_id`")
    @Size(max = 64, message = "应用ID（警信后台开户获取）最大长度要小于 64")
    @NotBlank(message = "应用ID（警信后台开户获取）不能为空")
    private String appId;

    /**
     * 应用密钥（警信后台开户获取）
     */
    @TableField(value = "`app_secret`")
    @Size(max = 255, message = "应用密钥（警信后台开户获取）最大长度要小于 255")
    @NotBlank(message = "应用密钥（警信后台开户获取）不能为空")
    private String appSecret;

    /**
     * 备注
     */
    @TableField(value = "`remark`")
    @Size(max = 500, message = "备注最大长度要小于 500")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField(value = "`created_by`")
    @NotNull(message = "创建人ID不能为null")
    private Long createdBy;

    /**
     * 是否默认入群用户，用于一键建群拉默认智能体。0：不；1：要
     */
    @TableField(value = "`default_user`")
    @NotNull(message = "是否默认入群用户，用于一键建群拉默认智能体。0：不；1：要不能为null")
    private Integer defaultUser;

    /**
     * 虚拟用户类型。1：智能体设备（智能体）；2：三方平台（双向）
     */
    @TableField(value = "`virtual_type`")
    private Integer virtualType;

    /**
     * 是否删除。0：未删除；1：删除
     */
    @TableField(value = "`deleted`")
    @NotNull(message = "是否删除。0：未删除；1：删除不能为null")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(value = "`created_at`")
    @NotNull(message = "创建时间不能为null")
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "`updated_at`")
    @NotNull(message = "更新时间不能为null")
    private Date updatedAt;
}