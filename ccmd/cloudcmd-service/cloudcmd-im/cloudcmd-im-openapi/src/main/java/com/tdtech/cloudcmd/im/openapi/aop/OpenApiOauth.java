package com.tdtech.cloudcmd.im.openapi.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface OpenApiOauth {

    BusinessScopeEnum value();

}
