package com.tdtech.cloudcmd.im.jingxin.client.entity;

/**
 * @author lsc
 * @date 2025/7/14
 **/
import lombok.Data;

@Data
public class UniPage {

    private Integer current; // 当前页码
    private Integer size; // 每页记录数
    private Integer total; // 总记录数
}
