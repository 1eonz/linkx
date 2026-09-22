package com.tdtech.cloudcmd.msip.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestLimit {
    /** 业务名称 */
    String business() default "";
    /** 超限提示 */
    String message() default "并发超限，请稍后再试";
}
