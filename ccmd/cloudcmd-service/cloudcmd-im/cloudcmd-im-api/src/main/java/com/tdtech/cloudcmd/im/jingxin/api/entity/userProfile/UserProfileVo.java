package com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tdtech.cloudcmd.util.json.JsonObject;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileVo {
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
    private List<JSONObject> appCustomSort;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;
}
