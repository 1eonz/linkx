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
@Accessors(chain = true)
@Document(indexName = "im_user")
public class ImUserES {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String code;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Keyword)
    private String avatar;

    @Field(type = FieldType.Keyword)
    private String gender;

    @Field(type = FieldType.Text)
    private String mobile;

    @Field(type = FieldType.Text)
    private String email;

    @Field(type = FieldType.Text)
    private String isdn;

    @Field(type = FieldType.Text)
    private String idCard;

    @Field(type = FieldType.Text)
    private String district;

    @Field(type = FieldType.Long)
    private Long directLeaderId;

    @Field(type = FieldType.Text)
    private String directLeaderName;

    @Field(type = FieldType.Keyword)
    private String departmentCode;

    @Field(type = FieldType.Text)
    private String departmentName;

    @Field(type = FieldType.Long)
    private Long departmentId;

    @Field(type = FieldType.Integer)
    private Integer type;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Text)
    private String password;

    @Field(type = FieldType.Date)
    private Date pwdTime;

    @Field(type = FieldType.Date)
    private Date gmtCreated;

    @Field(type = FieldType.Date)
    private Date gmtUpdated;
}
