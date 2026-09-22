package com.chinasoft.cloud.module.aiagent.controller.admin.vo;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
public class RecordCountVO {

    private Long agentId;

    private String agentName;

    // 智能体分类ID
    private List<Category> categorys;

    private String userName;

    private String identityCardNumber;

    private Long count;

    private String date;

    @JsonIgnore
    private Date dateTime;
}
