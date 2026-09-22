package com.tdtech.cloudcmd.auth.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * IM用户ES实体类
 * 存储明文数据，用于搜索
 */
@Data
public class ImUserDto {

    private Long id;

    private String code;

    private String name;

    private String avatar;

    private String gender;

    private String mobile;

    private String email;

    private String isdn;

    private String idCard;

    private String district;

    private Long directLeaderId;

    private String directLeaderName;

    private String departmentCode;

    private String departmentName;

    private Long departmentId;

    private Integer type;

    private Integer status;

    private String password;

    private Date pwdTime;

    private Date gmtCreated;

    private Date gmtUpdated;
}
