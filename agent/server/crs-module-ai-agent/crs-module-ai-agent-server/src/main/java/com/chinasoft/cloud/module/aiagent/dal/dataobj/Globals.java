package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinasoft.cloud.framework.mybatis.core.dataobject.BaseDO;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@TableName("ai_globals")
@KeySequence("ai_globals") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@TenantIgnore
public class Globals {

    @TableId
    private Long id;

    private String name;

    private String value;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态 0-可用 1-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 更新时间
     */
    private Date gmtModified;

}
