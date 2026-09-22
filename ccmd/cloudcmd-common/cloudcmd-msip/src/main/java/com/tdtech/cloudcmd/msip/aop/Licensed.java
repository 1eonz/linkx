package com.tdtech.cloudcmd.msip.aop;

import com.tdtech.cloudcmd.msip.enums.LicenseEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Licensed {
    /**
     * 需要检查的模块编码（枚举）
     * @see com.tdtech.cloudcmd.msip.enums.LicenseEnum
     * @return
     */
    LicenseEnum module();
    /** 受限提示 */
    String message() default "系统功能受限，请联系管理员";
}
