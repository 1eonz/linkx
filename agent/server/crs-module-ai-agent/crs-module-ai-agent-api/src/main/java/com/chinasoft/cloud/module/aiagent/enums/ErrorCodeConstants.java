package com.chinasoft.cloud.module.aiagent.enums;

import com.chinasoft.cloud.framework.common.exception.ErrorCode;

/**
 * System 错误码枚举类
 * system 系统，使用 1-002-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode AGENT_CONFIG_NOT_FOUND = new ErrorCode(6_001, "代理配置未找到");
    ErrorCode AGENT_CONFIG_DUPLICATED_TOKEN = new ErrorCode(6_002, "名称或者token和已有重复，请重新输入");
    ErrorCode CATEGORY_IN_USE = new ErrorCode(6_003, "分类正在使用，无法删除");
    ErrorCode APPROVAL_CONFIG_MISSING = new ErrorCode(6_004, "审批配置缺失，请检查审批是否开启且已配置建单地址和回调地址");
    ErrorCode APPROVAL_CREATE_FAILED = new ErrorCode(6_005, "审批建单失败");
    ErrorCode APPROVAL_RECORD_NOT_FOUND = new ErrorCode(6_006, "审批记录不存在");
    ErrorCode APPROVAL_USER_REQUIRED = new ErrorCode(6_007, "审批已开启时审批人不能为空");
    ErrorCode RECORD_NOT_FOUND = new ErrorCode(6_008, "问答记录不存在");

    // ========== 文件上传相关错误码 ==========
    ErrorCode FILE_TYPE_NOT_SUPPORTED = new ErrorCode(6_009, "不支持的文件格式");
    ErrorCode FILE_UPLOAD_FAILED = new ErrorCode(6_010, "文件上传失败");
    ErrorCode AI_FILE_UPLOAD_FAILED = new ErrorCode(6_011, "AI服务器文件上传失败");
    ErrorCode FILE_INFO_NOT_FOUND = new ErrorCode(6_012, "文件信息不存在或已过期，请重新上传文件");
    ErrorCode ATTACHMENT_CONFIG_NOT_FOUND = new ErrorCode(6_013, "文件上传接口配置不存在");
    ErrorCode FILE_HAS_ABNORMAL_DATA = new ErrorCode(6_020, "导入文件有异常数据");

    ErrorCode LINKX_USER_LOGIN_ERROR = new ErrorCode(6_014, "Linkx登录失败");
    ErrorCode LINKX_VIRTUAL_USER_LIST_ERROR = new ErrorCode(6_015, "获取虚拟用户列表失败");
    ErrorCode LINKX_AGENT_BIND_LIST_ERROR = new ErrorCode(6_016, "获取agent绑定的虚拟用户列表失败");
    ErrorCode LINKX_UPDATE_AGENT_ERROR = new ErrorCode(6_017, "更新agent绑定失败");
    ErrorCode LINKX_CREATE_AGENT_ERROR = new ErrorCode(6_018, "创建agent绑定失败");
    ErrorCode LINKX_USER_LOGOUT_ERROR = new ErrorCode(6_019, "Linkx退出失败");

}
