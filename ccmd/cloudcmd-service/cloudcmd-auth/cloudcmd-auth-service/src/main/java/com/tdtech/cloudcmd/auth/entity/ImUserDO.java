package com.tdtech.cloudcmd.auth.entity;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.mysql.encrypt.annotation.EncryptField;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("tb_im_user")
public class ImUserDO {

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

    /******** ICS属性 IM没得 ********/
    //用户类型：0 超级管理员 1 其他
    private Integer type;
    //是否锁定 1 是 0 否
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String password;
    private Date pwdTime;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Date gmtCreated;
    private Date gmtUpdated;

    @TableField(exist = false)
    private List<DepartmentNodeCustom> boundCustomDepartments;

    public List<DepartmentNodeCustom> getBoundCustomDepartments() {
        return boundCustomDepartments;
    }

    public void setBoundCustomDepartments(List<DepartmentNodeCustom> boundCustomDepartments) {
        this.boundCustomDepartments = boundCustomDepartments;
    }
}
