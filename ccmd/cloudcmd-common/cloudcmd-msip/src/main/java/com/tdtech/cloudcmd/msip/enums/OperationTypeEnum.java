package com.tdtech.cloudcmd.msip.enums;

/**
 * @author ly
 * @date 2025/8/28
 **/
public enum OperationTypeEnum {
    COLLABORATION_POST_INSERT("创建协同岗", "创建了%s协同岗", SubClassificationEnum.COLLABORATION_POST, LevelEnum.LEVEL_1),

    COLLABORATION_POST_UPDATE("修改协同岗", "修改了%s协同岗", SubClassificationEnum.COLLABORATION_POST, LevelEnum.LEVEL_2),

    COLLABORATION_POST_DELETE("删除协同岗", "删除了%s协同岗", SubClassificationEnum.COLLABORATION_POST, LevelEnum.LEVEL_2),

    AGENT_INSERT("创建智能体", "创建了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_1),

    AGENT_UPDATE("修改智能体", "修改了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_2),

    AGENT_DELETE("删除智能体", "删除了%s智能体", SubClassificationEnum.AGENT, LevelEnum.LEVEL_2),

    APPLICATION_INSERT("创建应用", "创建了%s应用", SubClassificationEnum.APPLICATION, LevelEnum.LEVEL_1),

    APPLICATION_UPDATE("修改应用", "修改了%s应用", SubClassificationEnum.APPLICATION, LevelEnum.LEVEL_2),

    APPLICATION_SHELVES("上架应用", "上架了%s应用", SubClassificationEnum.APPLICATION, LevelEnum.LEVEL_2),

    APPLICATION_DOWN_SHELF("下架应用", "下架了%s应用", SubClassificationEnum.APPLICATION, LevelEnum.LEVEL_2),

    APPLICATION_DELETE("删除应用", "删除了%s应用", SubClassificationEnum.APPLICATION, LevelEnum.LEVEL_2),

    LOGIN("登录", "%s登录", SubClassificationEnum.LOGIN, LevelEnum.LEVEL_1),

    LOGOUT("登出", "%s登出", SubClassificationEnum.LOGOUT, LevelEnum.LEVEL_1),

    LOCKED("账号冻结", "%s账号冻结了", SubClassificationEnum.FROZEN, LevelEnum.LEVEL_2),

    UPDATE_PASSWORD("修改密码", "%s修改了密码", SubClassificationEnum.UPDATE_PASSWORD, LevelEnum.LEVEL_2),

    RESET_PASSWORD("重置密码", "重置了%s密码", SubClassificationEnum.RESET_PASSWORD, LevelEnum.LEVEL_3),

    ROLE_INSERT("创建角色", "创建了%s角色，%s", SubClassificationEnum.ROLE_MANAGER, LevelEnum.LEVEL_3),

    ROLE_UPDATE("修改角色", "修改了%s角色，%s", SubClassificationEnum.ROLE_MANAGER, LevelEnum.LEVEL_3),

    ROLE_DELETE("删除角色", "删除了%s角色", SubClassificationEnum.ROLE_MANAGER, LevelEnum.LEVEL_3),

    ROLE_ENABLE("启用角色", "启用了%s角色", SubClassificationEnum.ROLE_MANAGER, LevelEnum.LEVEL_3),

    ROLE_DISABLE("禁用角色", "禁用了%s角色", SubClassificationEnum.ROLE_MANAGER, LevelEnum.LEVEL_3),

    PERMISSION_INSERT("创建权限", "创建了%s权限", SubClassificationEnum.PERMISSION_MANAGER, LevelEnum.LEVEL_3),

    PERMISSION_UPDATE("修改权限", "修改了%s权限", SubClassificationEnum.PERMISSION_MANAGER, LevelEnum.LEVEL_3),

    PERMISSION_DELETE("删除权限", "删除了%s权限", SubClassificationEnum.PERMISSION_MANAGER, LevelEnum.LEVEL_3),

    PERMISSION_ENABLE("启用权限", "启用了%s权限", SubClassificationEnum.PERMISSION_MANAGER, LevelEnum.LEVEL_3),

    PERMISSION_DISABLE("禁用权限", "禁用了%s权限", SubClassificationEnum.PERMISSION_MANAGER, LevelEnum.LEVEL_3),

    USER_INSERT("创建用户", "创建了%s用户", SubClassificationEnum.USER_MANAGER, LevelEnum.LEVEL_3),

    USER_UPDATE("修改用户", "修改了%s用户，变更明细：%s", SubClassificationEnum.USER_MANAGER, LevelEnum.LEVEL_3),

    USER_DELETE("删除用户", "删除了%s用户", SubClassificationEnum.USER_MANAGER, LevelEnum.LEVEL_3),

    USER_ENABLE("启用用户", "启用了%s用户", SubClassificationEnum.USER_MANAGER, LevelEnum.LEVEL_3),

    USER_DISABLE("禁用用户", "禁用了%s用户", SubClassificationEnum.USER_MANAGER, LevelEnum.LEVEL_3),

    GLOBAL_CONFIG_UPDATE("修改全局参数", "全局参数配置：{%s}已更新，变更详情：{%s}", SubClassificationEnum.GLOBAL_CONFIG_UPDATE, LevelEnum.LEVEL_3),

    GLOBAL_CONFIG_INSERT("新增全局参数", "新增了全局参数：{%s}", SubClassificationEnum.GLOBAL_CONFIG_UPDATE, LevelEnum.LEVEL_3),

    GLOBAL_CONFIG_DELETE("删除全局参数", "删除了全局参数：{%s}", SubClassificationEnum.GLOBAL_CONFIG_UPDATE, LevelEnum.LEVEL_3),

    GLOBAL_CONFIG_ENABLE("恢复全局参数", "恢复了%s全局参数", SubClassificationEnum.GLOBAL_CONFIG_UPDATE, LevelEnum.LEVEL_3),

    GROUP_ARCHIVE_EXPORT("导出群组归档数据", "导出了%s群组归档数据", SubClassificationEnum.GROUP_ARCHIVE_MANAGER, LevelEnum.LEVEL_3),

    GROUP_ARCHIVE_DELETE("删除群组归档数据", "删除了%s群组归档数据", SubClassificationEnum.GROUP_ARCHIVE_MANAGER, LevelEnum.LEVEL_3),

    LOCATION_INSERT("新增位置信息", "新增了%s位置信息", SubClassificationEnum.DEPT_LOCATION, LevelEnum.LEVEL_1),

    LOCATION_UPDATE("修改位置信息", "修改了%s位置信息", SubClassificationEnum.DEPT_LOCATION, LevelEnum.LEVEL_2),

    LOCATION_DELETE("删除位置信息", "删除了%s位置信息", SubClassificationEnum.DEPT_LOCATION, LevelEnum.LEVEL_2),

    POLICE_TICKET_CONFIG_INSERT("新增警单系统信息", "新增了%s警单系统信息", SubClassificationEnum.POLICE_TICKET_CONFIG, LevelEnum.LEVEL_1),

    POLICE_TICKET_CONFIG_UPDATE("修改警单系统信息", "修改了%s警单系统信息", SubClassificationEnum.POLICE_TICKET_CONFIG, LevelEnum.LEVEL_2),

    POLICE_TICKET_CONFIG_DELETE("删除警单系统信息", "删除了%s警单系统信息", SubClassificationEnum.POLICE_TICKET_CONFIG, LevelEnum.LEVEL_2),

    CAROUSEL_INSERT("新增资讯轮播图", "新增了%s资讯轮播图", SubClassificationEnum.CAROUSEL, LevelEnum.LEVEL_1),

    CAROUSEL_UPDATE("修改资讯轮播图", "修改了%s资讯轮播图", SubClassificationEnum.CAROUSEL, LevelEnum.LEVEL_2),

    CAROUSEL_DELETE("删除资讯轮播图", "删除了%s资讯轮播图", SubClassificationEnum.CAROUSEL, LevelEnum.LEVEL_2),

    COLLABORATION_GROUP_INSERT("新增协同群组", "新增了%s协同群组", SubClassificationEnum.COLLABORATION_GROUP, LevelEnum.LEVEL_1),

    COLLABORATION_GROUP_UPDATE("修改协同群组", "修改协了%s同群组", SubClassificationEnum.COLLABORATION_GROUP, LevelEnum.LEVEL_2),

    COLLABORATION_GROUP_DELETE("删除协同群组", "删除了%s协同群组", SubClassificationEnum.COLLABORATION_GROUP, LevelEnum.LEVEL_2),

    COLLABORATION_LABEL_INSERT("新增协同岗标签", "新增了%s协同岗标签", SubClassificationEnum.COLLABORATION_LABEL, LevelEnum.LEVEL_1),

    COLLABORATION_LABEL_UPDATE("修改协同岗标签", "修改了%s协同岗标签", SubClassificationEnum.COLLABORATION_LABEL, LevelEnum.LEVEL_2),

    COLLABORATION_LABEL_DELETE("删除协同岗标签", "删除了%s协同岗标签", SubClassificationEnum.COLLABORATION_LABEL, LevelEnum.LEVEL_2),

    GROUP_LABEL_INSERT("新增群组标签", "新增了%s群组标签", SubClassificationEnum.GROUP_LABEL, LevelEnum.LEVEL_1),

    GROUP_LABEL_UPDATE("修改群组标签", "修改了%s群组标签", SubClassificationEnum.GROUP_LABEL, LevelEnum.LEVEL_2),

    GROUP_LABEL_DELETE("删除群组标签", "删除了%s群组标签", SubClassificationEnum.GROUP_LABEL, LevelEnum.LEVEL_2),

    GROUP_SYSTEM_UPDATE("修改了系统配置", "系统设置管理 - 公共设置已更新，%s", SubClassificationEnum.SYSTEM_CONFIG, LevelEnum.LEVEL_2),

    GROUP_SYSTEM_DELETE("删除了系统配置", "删除了系统配置：%s", SubClassificationEnum.SYSTEM_CONFIG, LevelEnum.LEVEL_2),

    PC_CONFIG_UPDATE("修改了PC端设置", "系统设置管理 - PC端设置已更新，%s", SubClassificationEnum.PC_CONFIG, LevelEnum.LEVEL_2),

    GROUP_APPH5_INSERT("新增了APPH5布局配置", "新增了APPH5布局配置：{%s}", SubClassificationEnum.APPH5_CONFIG, LevelEnum.LEVEL_1),

    GROUP_APPH5_UPDATE("修改了APPH5布局配置", "修改了APPH5布局配置：{%s}", SubClassificationEnum.APPH5_CONFIG, LevelEnum.LEVEL_2),

    GROUP_APPH5_DELETE("删除了APPH5布局配置", "删除了APPH5布局配置：{%s}", SubClassificationEnum.APPH5_CONFIG, LevelEnum.LEVEL_2),

    COLLABORATION_FUNCTIONALDEPTS_INSERT("创建职能部门", "创建新职能部门：{%s}", SubClassificationEnum.FUNCTIONALDEPTS_CONFIG, LevelEnum.LEVEL_1),

    COLLABORATION_FUNCTIONALDEPTS_UPDATE("修改职能部门", "修改了职能部门：{%s}", SubClassificationEnum.FUNCTIONALDEPTS_CONFIG, LevelEnum.LEVEL_2),

    COLLABORATION_FUNCTIONALDEPTS_DELETE("删除职能部门", "删除了职能部门：{%s}", SubClassificationEnum.FUNCTIONALDEPTS_CONFIG, LevelEnum.LEVEL_2),

    ICP_CONFIG_UPDATE("更新融合通信配置", "融合通信服务器配置已更新:%s", SubClassificationEnum.ICP_CONFIG, LevelEnum.LEVEL_3),

    ICP_CONFIG_INSERT("新增融合通信配置",
            "融合通信服务器初始配置已完成：协议类型：{%s}，ICP服务器IP：{%s}，ICP服务器端口：{%s}，Websocket地址：{%s}，登录账号：{%s}，登录密码：{******}",
            SubClassificationEnum.ICP_CONFIG, LevelEnum.LEVEL_3),

    EQUIPMENT_AUTH_UPDATE("更新设备调度权限", "用户{%s}的设备调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    CAMERA_AUTH_UPDATE("更新摄像头调度权限", "用户{%s}的摄像头调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    EQUIPMENT_BATCH_AUTH_UPDATE("批量更新设备调度权限", "用户{%s}的设备调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    CAMERA_BATCH_AUTH_UPDATE("批量更新摄像头调度权限", "用户{%s}的摄像头调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    EQUIPMENT_DEPT_AUTH_UPDATE("批量更新部门设备调度权限", "部门{%s}下人员的设备调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    CAMERA_DEPT_AUTH_UPDATE("批量更新部门摄像头调度权限", "部门{%s}下人员的摄像头调度调度权限已更新，授权范围：%s", SubClassificationEnum.ICP_AUTH, LevelEnum.LEVEL_3),

    COOP_LEVEL_INSERT("新增协同岗层级", "新增了协同层级:%s", SubClassificationEnum.COOP_LEVEL, LevelEnum.LEVEL_1),

    COOP_LEVEL_UPDATE("修改协同岗层级", "修改了协同层级:%s -> %s", SubClassificationEnum.COOP_LEVEL, LevelEnum.LEVEL_1),

    COOP_LEVEL_DELETE("删除协同岗层级", "删除了协同层级%s及其所属子层级", SubClassificationEnum.COOP_LEVEL, LevelEnum.LEVEL_1),

    COOP_LEVEL_MEMBER_UPDATE("挂靠协同岗", "挂靠协同岗%s至协同岗层级%s", SubClassificationEnum.COOP_LEVEL, LevelEnum.LEVEL_1),

    COOP_LEVEL_MEMBER_DELETE("删除协同岗层级", "删除了协同岗层级%s下挂靠的协同岗%s", SubClassificationEnum.COOP_LEVEL, LevelEnum.LEVEL_1),

    DUTY_SCHEDULE_IMPORT("导入值班信息", "导入了%d条值班信息", SubClassificationEnum.DUTY_SCHEDULE, LevelEnum.LEVEL_1),
    GROUP_THIRD_APP_INSERT("新增了三方对接南向应用", "新增了三方对接南向应用: %s", SubClassificationEnum.SOUTH_APP, LevelEnum.LEVEL_2),
    GROUP_THIRD_APP_UPDATE("修改了三方对接南向应用", "修改了三方对接南向应用：\"%s【%s】\",%s", SubClassificationEnum.SOUTH_APP, LevelEnum.LEVEL_2),
    GROUP_THIRD_APP_MAPPER_UPDATE("修改了三方对接南向应用的映射配置", "修改了三方对接南向应用\"%s【%s】\"的映射配置为：%s", SubClassificationEnum.SOUTH_APP, LevelEnum.LEVEL_2),
    GROUP_THIRD_APP_TASK_CONFIG_UPDATE("修改了三方对接南向应用的任务标准件派发配置", "修改了三方对接南向应用\"%s【%s】\"的任务标准件派发配置为：开启=%s，配置=%s", SubClassificationEnum.SOUTH_APP, LevelEnum.LEVEL_2),
    GROUP_THIRD_APP_DELETE("删除了三方对接南向应用", "删除了三方对接南向应用:\"%s【%s】\"", SubClassificationEnum.SOUTH_APP, LevelEnum.LEVEL_2),

    VIRTUAL_USER_INSERT("新增虚拟用户", "新增了虚拟用户:%s", SubClassificationEnum.VIRTUAL_USER, LevelEnum.LEVEL_2),
    VIRTUAL_USER_UPDATE("修改虚拟用户", "修改了虚拟用户:%s", SubClassificationEnum.VIRTUAL_USER, LevelEnum.LEVEL_2),
    VIRTUAL_USER_DELETE("删除虚拟用户", "删除了虚拟用户:%s", SubClassificationEnum.VIRTUAL_USER, LevelEnum.LEVEL_2),

    P2P_SERVER_INSERT("创建服务器节点", "创建了服务器节点：%s", SubClassificationEnum.P2P_NODE, LevelEnum.LEVEL_1),
    P2P_SERVER_DELETE("删除服务器节点", "删除了服务器节点：%s", SubClassificationEnum.P2P_NODE, LevelEnum.LEVEL_2),
    P2P_SERVER_GRANT_UPDATE("更新服务器开放数据授权", "更新了服务器开放数据授权：%s", SubClassificationEnum.OPEN_DATA, LevelEnum.LEVEL_2),
    P2P_CLIENT_UPDATE("更新客户端节点", "更新了客户端节点：%s", SubClassificationEnum.P2P_NODE, LevelEnum.LEVEL_2),
    P2P_CLIENT_DELETE("删除客户端节点", "删除了客户端节点：%s", SubClassificationEnum.P2P_NODE, LevelEnum.LEVEL_2),
    P2P_CLIENT_GRANT_UPDATE("更新客户端开放数据授权", "更新了客户端开放数据授权：%s", SubClassificationEnum.OPEN_DATA, LevelEnum.LEVEL_2),
    P2P_SERVER_UPDATE("更新服务器节点", "更新了服务器节点：%s", SubClassificationEnum.P2P_NODE, LevelEnum.LEVEL_2);

    private String name;
    private String desc;
    private SubClassificationEnum parent;
    private LevelEnum level;

    OperationTypeEnum(String name, String desc, SubClassificationEnum parent, LevelEnum level) {
        this.name = name;
        this.desc = desc;
        this.parent = parent;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public SubClassificationEnum getParent() {
        return parent;
    }
}