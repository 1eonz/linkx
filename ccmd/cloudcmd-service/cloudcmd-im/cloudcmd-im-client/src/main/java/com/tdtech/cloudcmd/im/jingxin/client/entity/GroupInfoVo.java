package com.tdtech.cloudcmd.im.jingxin.client.entity;

import com.tdtech.cloudcmd.util.StringUtils;
import lombok.Data;

/**
 * @author: S063874
 * @date: 2026-01-15 18:40
 */
@Data
public class GroupInfoVo {

    private String avatar;

    private Long gmtCreated;

    private Long gmtModified;

    private Long groupId;

    private Integer type;

    private String undefinedName;

    private String name;

    private Owner owner;

    public String getName() {
        if (StringUtils.isBlank(name)) {
            return undefinedName;
        }
        return name;
    }

    @Data
    static class Owner{
        String departmentCode;
        Long departmentId;
        String departmentName;
        String fullPath;
        String fullPathCode;
        String fullPathName;
        String mobile;
        String name;
        Long userId;
    }



}
