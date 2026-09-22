package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用)
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
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
    public static final String IS_TIMEOUT_LOGOUT = "is_timeout_logout";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    private static final String IS_PUBLISH_ONLINE = "is_publish_online";
    private static final String IS_MULTI_LOGIN = "is_multi_login";
    /**
     * 主键
     */
    private Long id;
    /**
     * 应用名称
     */
    private String name;
    /**
     * 应用KEY .区分不同的应用.获取不同应用的权限.
     */
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
     * 应用类型：0-内部应用1-外部应用
     */
    private Integer type;
    /**
     * 是否发布上线通知
     */
    private String isPublishOnline;
    /**
     * 是否允许多登陆
     */
    private Integer isMultiLogin;
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

    public boolean isCapp() {
        return Objects.equals("CAPP-1000", appKey);
    }

    public boolean isIcc() {
        return Objects.equals("CDC-1000", appKey);
    }

    public boolean isAdmin() {
        return Objects.equals("CDC-2000", appKey);
    }
}
