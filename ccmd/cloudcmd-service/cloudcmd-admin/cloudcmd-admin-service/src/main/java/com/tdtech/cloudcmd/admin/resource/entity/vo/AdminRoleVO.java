package com.tdtech.cloudcmd.admin.resource.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AdminRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private Integer type;

    private List<String> iccPrivJson;

    private List<String> cappPrivJson;

    private List<String> adminPrivJson;

    private List<OrgPrivDto> orgPrivList;

    private Integer status = 0;

    private Date gmtCreated;

    private Date gmtModified;
}