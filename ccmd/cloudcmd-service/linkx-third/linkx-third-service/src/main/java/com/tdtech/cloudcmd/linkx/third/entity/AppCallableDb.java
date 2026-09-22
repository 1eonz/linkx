package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 南向应用数据库注册信息表
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_app_callable_db")
public class AppCallableDb implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * tb_app_callable.id
     */
    private String appCallableId;

    /**
     * 数据库类型。1：MYSQL；2：Oracle；3：SQLServer等
     */
    private Integer dbType;

    /**
     * 数据库连接账号
     */
    private String account;

    /**
     * 数据库连接密码
     */
    private String password;

    /**
     * 数据库库名
     */
    private String databaseName;

    /**
     * 数据库表名或视图名
     */
    private String dataName;
}
