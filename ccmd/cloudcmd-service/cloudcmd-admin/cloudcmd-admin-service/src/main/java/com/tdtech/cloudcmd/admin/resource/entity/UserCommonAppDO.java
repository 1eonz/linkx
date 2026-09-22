package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@TableName("user_common_app")
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCommonAppDO {

    /**
     * 应用编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private String userId;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 排序值
     */
    private Integer sort = 0;
    /**
     * 终端类型
     */
    private Integer terminalType;


    @TableField(exist = false)
    private AppInfoDO app;
}
