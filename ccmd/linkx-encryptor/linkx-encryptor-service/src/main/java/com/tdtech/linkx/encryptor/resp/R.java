package com.tdtech.linkx.encryptor.resp;


import com.tdtech.linkx.encryptor.enums.ResponseCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 响应对象
 * 
 * { "code": 1, "msg": "XXXXX" , "data": ... }
 * 
 *
 * //将该标记放在属性上，如果该属性为NULL则不参与序列化 //如果放在类上边,那对这个类的全部属性起作用 //Include.Include.ALWAYS 默认 //Include.NON_DEFAULT 属性为默认值不序列化
 * //Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化 //Include.NON_NULL 属性为NULL 不序列化
 * 
 * @author zhangweibo
 * @since 1.0.0
 */
@NoArgsConstructor
@Data
@ToString
public class R<T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected int code;

    protected String msg;

    protected T data;

    private R(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    private R(int code, String msg) {
        this(code, null, msg);
    }

    /**
     * 通用成功
     */
    public static <T> R<T> success() {
        return new R<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMsg());
    }

    /**
     * 成功后有返回对象
     */
    public static <T> R<T> success(T data) {
        return new R<>(ResponseCodeEnum.SUCCESS.getCode(), data, ResponseCodeEnum.SUCCESS.getMsg());
    }

    public static <T> R<T> success(int code, String message, T data) {
        return new R<>(code, data, message);
    }

    public static <T> R<T> success(int code, String message) {
        return new R<>(code, message);
    }

    /**
     * 失败后有返回对象
     */
    public static <T> R<T> failure() {
        return new R<>(ResponseCodeEnum.FAILURE.getCode(), ResponseCodeEnum.FAILURE.getMsg());
    }

    /**
     * 失败后有返回对象
     */
    public static <T> R<T> failure(T data) {
        return new R<>(ResponseCodeEnum.FAILURE.getCode(), data, ResponseCodeEnum.FAILURE.getMsg());

    }

    /**
     * 主要显示业务逻辑上的失败
     */
    public static <T> R<T> failure(String message) {
        return new R<>(ResponseCodeEnum.FAILURE.getCode(), message);
    }

    /**
     * 具体的系统错误, 且有返回对象
     */
    public static <T> R<T> failure(int code, String message, T data) {
        return new R<>(code, data, message);
    }

    /**
     * 具体的系统错误
     */
    public static <T> R<T> failure(int code, String message) {
        return new R<>(code, message);
    }

}
