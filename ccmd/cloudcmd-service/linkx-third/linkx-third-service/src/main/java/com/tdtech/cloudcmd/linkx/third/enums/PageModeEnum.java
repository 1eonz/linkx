package com.tdtech.cloudcmd.linkx.third.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分页模式
 *
 */
@AllArgsConstructor
@Getter
public enum PageModeEnum {

    PAGE(1, "页码模式"),

    OFFSET(2, "偏移量模式(游标模式)");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 信息
     */
    private final String msg;
}
