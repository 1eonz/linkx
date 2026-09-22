package com.tdtech.cloudcmd.msip.aop;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface LogReportParam {

    /** 字段名，支持嵌套，如 dept.name */
    String field() default "name";
    /** 是否需要国际化处理 */
    boolean international() default false;
    /** 拼接分隔符 */
    String delimiter() default ",";
}
