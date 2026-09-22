package com.tdtech.cloudcmd.license.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {

    private String code;

    private String msg;

    private T data;

}
