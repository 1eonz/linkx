package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用)
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_application")
public class Application implements Serializable {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String APP_KEY = "app_key";
    public static final String APP_SECRET = "app_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String TYPE = "type";
    public static final String IS_PUBLISH_ONLINE = "is_publish_online";
    public static final String IS_TIMEOUT_LOGOUT = "is_timeout_logout";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 领用记录ID
     */
    private Long id;
    /**
     * 应用名称
     */
    @NotBlank
    private String name;
    /**
     * 应用KEY .区分不同的应用.获取不同应用的权限.
     */
    @NotBlank
    private String appKey;
    /**
     * 应用密钥
     */
    private String appSecret;
    /**
     * 回调URL
     */
    private String redirectUri;
    /**
     * 应用类型
     */
    private Integer type;
    /**
     * 是否发布上线通知
     */
    private Integer isPublishOnline;
    /**
     * 是否超时退出
     */
    private Integer isTimeoutLogout;
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
