package com.tdtech.cloudcmd.linkx.third.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 南向应用vo
 */
@Data
public class AppCallableVo {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 所属系统
     */
    private String systemName;

    /**
     * 系统编码（系统分配）
     */
    private String systemCode;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 应用类型。1：接口调用型（default）；2：数据库访问型；
     */
    private Integer type;

    /**
     * 展示范围。0：全部；1：PC；2：移动端
     */
    private Integer scope;

    /**
     * 访问IP
     */
    private String ip;

    /**
     * 访问端口
     */
    private Integer port;

    /**
     * 执行周期。单位：分钟
     */
    private Integer period;

    /**
     * 响应的字段与用户可读字段映射信息，数据结构主要为key:value形式
     */
    private String mapper;

    /**
     * 是否开启任务标准件派发。0：否（默认）；1：是
     */
    private Integer enableTask;

    /**
     * 任务标准件派发时的自动表单回填配置，JSON 串
     */
    private String taskAutoFillConfig;

    /**
     * 唯一标识字段
     */
    private String uniqueId;

    /**
     * 是否删除。0：未删除（默认）；1：已删除
     */
    private Integer isDeleted;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime gmtCreated;
}