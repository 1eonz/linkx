package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.PWICodeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ResponseObject<T> {

    @JsonProperty("rsp")
    private PWICodeEnum code;

    private List<T> list;

    private T value;

    private Integer totalNum;

}
