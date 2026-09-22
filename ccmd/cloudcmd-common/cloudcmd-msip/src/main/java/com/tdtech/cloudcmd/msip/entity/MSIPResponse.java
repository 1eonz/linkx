package com.tdtech.cloudcmd.msip.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class MSIPResponse<T> {
    private String resultCode;
    private String msg;
    private T datas;
}
