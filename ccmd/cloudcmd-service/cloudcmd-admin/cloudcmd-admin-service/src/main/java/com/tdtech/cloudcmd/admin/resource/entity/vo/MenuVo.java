package com.tdtech.cloudcmd.admin.resource.entity.vo;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2021/1/12 16:27
 */
@Data
public class MenuVo implements Serializable {

    /**
     * 应用id
     */
    private Long applicationId;

    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 角色类别
     */
    private Integer type;

    /**
     * 有选中的菜单id，用于权限处理
     */
    private List<Long> menuIdList;
}
