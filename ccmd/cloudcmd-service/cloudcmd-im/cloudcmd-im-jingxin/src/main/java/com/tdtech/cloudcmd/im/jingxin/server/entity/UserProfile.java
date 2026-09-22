package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户偏好信息表
 *
 * @author wb
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "tb_user_profile", autoResultMap = true)
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * App展示方式。1：按照热度；2：自定义顺序
     */
    private Integer appSortType;

    /**
     * App被用户手动了展示顺序的列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<JSONObject> appCustomSort;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
}
