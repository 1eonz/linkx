package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/18 18:31
 */
@Data
public class ExtendInfoPropertiesQueryParam implements Serializable {

    private String code;

    private String name;
}
