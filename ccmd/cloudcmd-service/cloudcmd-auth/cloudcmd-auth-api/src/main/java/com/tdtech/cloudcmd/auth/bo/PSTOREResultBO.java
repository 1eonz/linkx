package com.tdtech.cloudcmd.auth.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author michstabe
 * @date 2023/6/12 15:31
 * @description 三方响应实体类
 */

@Data
public class PSTOREResultBO implements Serializable {

    /**
     * 响应码
     */
    private String ret;

    /**
     * 响应描述
     */
    private String msg;
}
