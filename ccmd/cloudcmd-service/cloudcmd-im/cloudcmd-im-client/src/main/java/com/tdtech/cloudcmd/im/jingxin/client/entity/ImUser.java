package com.tdtech.cloudcmd.im.jingxin.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ImUser {

    private Long id;
    private String code;
    private String name;
    private String avatar;
    private String gender;
    private String mobile;
    private String email;
    private String isdn;
    private String idCard;
    private String district;
    private Long directLeaderId;
    private String directLeaderName;
    private Integer isBinding = 0;
    private Integer status;
    private List<UserDepartment> userDepartments;
    private List<UserType> userTypes;

    @Getter
    @Setter
    @ToString
    public static class UserDepartment {
        private String departmentCode;
        private String departmentName;
        private Boolean isPrimary;
        private String positionName;
        private Integer sort;
        @JsonProperty("departmentId")//僵硬。。。别个不叫id
        private Long id;
        /**
         * 全路径-id
         */
        private String fullPath;
        /**
         * 全路径-code
         */
        private String fullPathCode;

        /**
         * 全路径-名称
         */
        private String fullPathName;
    }

    @Getter
    @Setter
    @ToString
    public static class UserType {
        private String typeId;
        private String typeName;
    }

    public UserDepartment getPrimaryDepartment() {
        if (userDepartments == null || userDepartments.isEmpty()) {
            return null;
        }
        return userDepartments.stream().filter(UserDepartment::getIsPrimary).findAny().orElse(null);
    }
}
