package com.tdtech.linkx.encryptor.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCodeEnum {
    SUCCESS(0, "操作成功"),
    FAILURE(1, "操作失败");
    private int code;
    private String msg;
}
