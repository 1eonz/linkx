package com.tdtech.cloudcmd.im.jingxin.client.entity;

/**
 * @author lsc
 * @date 2025/7/14
 **/

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
public class UserListVo {

    private String id; // ID
    private String name; // 姓名
    private String avatar; // 头像原图 fileID
    private String isdn; // 通讯号码
    private Integer category; // 用户类型（数据字典定义）1-三方平台用户 2-智能助手用户
    private String categoryName; // 用户类型名称
    private UserDepartmentVo department; // 所属部门 (表1-334)
    private String remark; // 备注（签名）
    private List<BindUserVo> bindUsers; // 绑定人员列表对象 (表1-335)
    private UserStatVo statistics; // 协同岗统计信息 (表1-336)
    private List<BindingGroup> groups;
    private Long gmtCreated; // 创建时间
    private Long gmtModified; // 最后修改时间

    @Getter
    @Setter
    @ToString
    public static class BindingGroup {
        private Long groupId;
        private Long supportUserId;
        private String supportUserIdCard;
        private String supportUserName;

        public boolean hasSupportUser() {
            return supportUserId != null && supportUserId != 0L;
        }
    }
}
