package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserFailVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idCard;

    private Integer code;

    private String errMsg;
}
