package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 人员分页查询结果（auth 端）。
 */
@Data
public class ImUserPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNo;
    private Integer pageSize;
    private Integer total;
    private List<ImUserVO> records;
}