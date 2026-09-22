package com.tdtech.cloudcmd.admin.offlinemap.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseMapVo implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 序号
     */
    private Long number;

    /**
     * 底图文件名称
     */
    private String name;

    /**
     * 底图文件大小
     */
    private String size;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 瓦片地址
     */
    private String tiles;

    /**
     * 当前页数
     */
    private Integer pageNo;

    /**
     * 每页显示数据条数
     */
    private Integer pageSize;
}
