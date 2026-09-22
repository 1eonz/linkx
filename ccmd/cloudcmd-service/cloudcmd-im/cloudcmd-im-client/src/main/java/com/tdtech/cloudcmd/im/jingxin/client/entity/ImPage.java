package com.tdtech.cloudcmd.im.jingxin.client.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImPage<T> {
    private Integer current;
    private Integer size;
    private Integer total;
    private List<T> records;
    private Integer totalCount;
    private Integer pageNo;
    private Integer pageSize;
}
