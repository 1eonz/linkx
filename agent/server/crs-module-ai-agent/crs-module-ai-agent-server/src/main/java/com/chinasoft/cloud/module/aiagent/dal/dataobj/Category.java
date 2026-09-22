package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinasoft.cloud.framework.mybatis.core.dataobject.BaseDO;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("ai_agent_category")
@KeySequence("ai_agent_category") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class Category extends BaseDO {

    @TableId
    private Long id;

    private String name;

}
