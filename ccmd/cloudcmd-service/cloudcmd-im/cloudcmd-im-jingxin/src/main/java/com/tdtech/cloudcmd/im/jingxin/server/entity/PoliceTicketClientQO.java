package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.util.Date;
import lombok.Data;

@Data
public class PoliceTicketClientQO {

    /* ===== 基本查询条件 ===== */

    /**
     * 名称模糊查询
     */
    private String name;

    /**
     * 系统名称模糊查询
     */
    private String systemName;

    /**
     * 系统编码模糊查询
     */
    private String systemCode;

    /**
     * 协议模式模糊查询
     */
    private String schema;

    /**
     * IP地址模糊查询
     */
    private String ip;

    /**
     * 端口号精确查询
     */
    private Integer port;


    /**
     * URI路径模糊查询
     */
    private String path;

    /**
     * 创建人ID精确查询
     */
    private Long creator;

    /**
     * 主键ID精确查询
     */
    private Long id;

    /**
     * 脚本内容模糊查询
     */
    private String script;

    /* ===== 时间范围查询 ===== */

    /**
     * 创建时间范围-开始
     */
    private Date gmtCreatedStart;

    /**
     * 创建时间范围-结束
     */
    private Date gmtCreatedEnd;

    private Integer status;

}
