package com.tdtech.cloudcmd.msip.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum SubClassificationEnum {
    COLLABORATION_POST("协同岗管理", ClassificationEnum.RESOURCE),

    AGENT("智能体", ClassificationEnum.RESOURCE),

    APPLICATION("应用管理", ClassificationEnum.RESOURCE),

    CAROUSEL("资讯轮播图", ClassificationEnum.RESOURCE),

    COLLABORATION_GROUP("协同群组", ClassificationEnum.RESOURCE),

    COLLABORATION_LABEL("协同岗标签管理", ClassificationEnum.RESOURCE),

    GROUP_LABEL("群组标签管理", ClassificationEnum.RESOURCE),

    POLICE_TICKET_CONFIG("警单系统信息", ClassificationEnum.THIRD),

    LOGIN("登录", ClassificationEnum.ACCOUNT),

    LOGOUT("登出", ClassificationEnum.ACCOUNT),

    FROZEN("账户冻结", ClassificationEnum.ACCOUNT),

    UPDATE_PASSWORD("修改密码", ClassificationEnum.ACCOUNT),

    RESET_PASSWORD("重置密码", ClassificationEnum.ACCOUNT),

    USER_MANAGER("用户管理", ClassificationEnum.ACCOUNT),

    ROLE_MANAGER("角色管理", ClassificationEnum.PERMISSION),

    PERMISSION_MANAGER("权限管理", ClassificationEnum.PERMISSION),

//    ROLE_USER_RELATIONSHIP("角色用户关联", ClassificationEnum.PERMISSION),
//
//    ROLE_FUNCTION_RELATIONSHIP("角色功能关联", ClassificationEnum.PERMISSION),
//
//    COLLABORATION_POST_PERMISSION("协同岗管理权限", ClassificationEnum.PERMISSION),

    AI_PERMISSION("AI访问权限", ClassificationEnum.PERMISSION),

    MONITOR_PERMISSION("监控访问权限", ClassificationEnum.PERMISSION),

    GLOBAL_CONFIG_UPDATE("全局参数配置", ClassificationEnum.SYSTEM),

    GROUP_ARCHIVE_MANAGER("已归档群组管理", ClassificationEnum.SYSTEM),

    DEPT_LOCATION("部门默认位置信息管理", ClassificationEnum.SYSTEM),

    SYSTEM_CONFIG("后台系统配置", ClassificationEnum.SYSTEM),

    PC_CONFIG("PC端设置", ClassificationEnum.SYSTEM),

    APPH5_CONFIG("APPH5配置", ClassificationEnum.SYSTEM),

    FUNCTIONALDEPTS_CONFIG("职能分类管理", ClassificationEnum.SYSTEM),

    ICP_CONFIG("融合通信服务器配置", ClassificationEnum.SYSTEM),

    ICP_AUTH("融合通信授权管理", ClassificationEnum.PERMISSION),

    COOP_LEVEL("协同岗层级管理", ClassificationEnum.RESOURCE),
    SOUTH_APP("南向应用对接", ClassificationEnum.THIRD),

    DUTY_SCHEDULE("值班信息管理", ClassificationEnum.RESOURCE),

    VIRTUAL_USER("虚拟用户管理", ClassificationEnum.JINGXIN),

    P2P_NODE("节点管理", ClassificationEnum.RESOURCE),

    OPEN_DATA("数据管理", ClassificationEnum.RESOURCE);

    private String name;
    private ClassificationEnum parent;

    SubClassificationEnum(String name, ClassificationEnum parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public ClassificationEnum getParent() {
        return parent;
    }
}
