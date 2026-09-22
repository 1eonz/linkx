package com.tdtech.cloudcmd.msip.aop;

import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogReport {
    /**
     * @see com.tdtech.cloudcmd.msip.enums.OperationTypeEnum
     * @return
     */
    OperationTypeEnum type();
}
