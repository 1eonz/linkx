package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/18 18:19
 */
@Data
public class ItemQueryParam implements Serializable {

    private String type;

    private String value;
}
