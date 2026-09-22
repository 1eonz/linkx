package com.tdtech.cloudcmd.mysql.encrypt.annotation;

import java.lang.annotation.*;

/**
 * 敏感字段加密注解
 * 标记在实体类的字段上，表示该字段需要加密存储
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptField {
    
    /**
     * 是否加密（默认true）
     * 可以设置为false来临时禁用加密
     */
    boolean value() default true;
}
