package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>
 * 全局变量信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Data
public class GlobalsDto implements Serializable {

    /**
     * 变量名称
     */
    private String name;

    /**
     * 值
     */
    private String value;

}
