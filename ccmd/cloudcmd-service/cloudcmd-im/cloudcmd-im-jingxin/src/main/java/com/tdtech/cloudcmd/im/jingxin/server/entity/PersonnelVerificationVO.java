package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

@Data
public class PersonnelVerificationVO {

    /**
     * 日期
     */
    private String date;

    /**
     * 次数
     */
    private Long count = 0L;

}
