package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 警信人员分页查询结果（RPC 传输对象）。
 */
@Data
public class ImUserPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNo;
    private Integer pageSize;
    private Integer total;
    private List<ImUserVo> records;
}