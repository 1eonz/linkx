package com.tdtech.cloudcmd.linkx.third.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 南向应用创建DTO
 */
@Data
public class AppCallableCreateDto {
    /**
     * 系统编码（系统分配）
     */
    @NotBlank
    private String systemCode;

    /**
     * 所属系统
     */
    @NotBlank
    private String systemName;

    /**
     * 接口名称
     */
    @NotBlank
    private String name;

    /**
     * 应用类型。1：接口调用型（default）；2：数据库访问型；
     */
    @NotNull
    private Integer type;

    /**
     * 展示范围。0：全部；1：PC；2：移动端
     */
    @NotNull
    private Integer scope;

    /**
     * 访问IP
     */
    @NotBlank
    private String ip;

    /**
     * 访问端口
     */
    @NotNull
    private Integer port;

    /**
     * 唯一标识字段
     */
    @NotNull
    private String uniqueId;

    /**
     * 是否支持分页 1：分页；0：不支持分页（default）
     */
    private Integer pagenation;

    /**
     * 分页模式 1：页码模式；2：偏移模式；默认页码模式
     */
    private Integer pagenationType;

    /**
     * 分页参数位置 1：Body参数；0：Query参数（default）
     */
    private Integer pageParamLocation;

    /**
     * 如果支持分页时，页码的参数名称
     */
    private String pageFieldName;

    /**
     * 如果支持分页时，每页条目数的参数名称
     */
    private String pageSizeFieldName;

    /**
     * API响应结果路径
     */
    private String responseDataPath;

    /**
     * 三方数据库的第一条数据开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataStartTime;

    /**
     * 三方数据库的代表时间的字段-用于查询数据索引
     */
    private String dateTimeSign;

    /**
     * 访问协议。https/http/ws/wss
     */
    private String protocol;

    /**
     * 请求方法，例如GET/POST/PUT/DELETE等；
     */
    private String method;

    /**
     * 请求头，key:value的json串
     */
    private String reqHeader;

    /**
     * 请求体，key:value的json串
     */
    private String reqBody;

    /**
     * 请求参数
     */
    private String reqParam;

    /**
     * 访问URI地址
     */
    private String uri;

    /**
     * 执行周期。单位：分钟
     */
    @NotNull
    private Integer period;

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
     * 数据库表名或视图名
     */
    private String dataName;

    /**
     * 数据库库名
     */
    private String databaseName;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}