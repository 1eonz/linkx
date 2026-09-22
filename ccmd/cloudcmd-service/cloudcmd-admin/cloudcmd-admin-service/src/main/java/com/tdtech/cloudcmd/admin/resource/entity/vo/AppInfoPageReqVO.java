package com.tdtech.cloudcmd.admin.resource.entity.vo;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class AppInfoPageReqVO {

    private Long currentId;

    private String name;
    // 新增字段：是否启用
    private Integer status;

    private Integer pageNum;

    private Integer pageSize;

    private Integer scope;
}