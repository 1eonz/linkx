package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OneOver1p4BCallResp {

    @JsonProperty("gmsfhm")
    private String idCard;

    private String name;

    private String photo;

}
