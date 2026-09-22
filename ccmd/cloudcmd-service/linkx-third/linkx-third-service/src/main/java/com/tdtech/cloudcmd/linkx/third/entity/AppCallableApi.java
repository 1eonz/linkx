package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 南向应用接口注册信息表
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_app_callable_api")
public class AppCallableApi implements Serializable {

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
    private LocalDateTime dataStartTime;


    /**
     * 三方数据库的代表时间的字段-用于查询数据索引
     */
    private String dateTimeSign;
}
