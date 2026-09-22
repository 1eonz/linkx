package com.tdtech.cloudcmd.auth.enums;

/**
 * @author
 * @date 2019-08-24 16:19
 * @description
 */
public enum ResultCode {

    /*参数说明*/

    /**
     * 响应状态码定义
     */
    // 操作成功
    PASSPORT_SUCCESS(1, "操作成功"), PASSPORT_CHECK_SUCCESS(0, "ResultCode_PASSPORT_CHECK_SUCCESS"),
    LOGIN_SUCCESS(1, "登录成功"), PASSPORT_LOGOUT_SUCCESS(1, "登出成功"), PASSPORT_BIND_SUCCESS(1, "账户资源绑定成功"),
    PASSPORT_GET_STATUS_SUCCESS(1, "获取账户状态成功"), PASSPORT_UNBIND_SUCCESS(1, "解绑成功"),

    // 操作失败
    PASSPORT_FAILURE(0, "操作失败"), PASSPORT_CHECK_FAILURE(0, "验证失败"),
    // 无法确定错误级别
    LOGIN_FAILURE(0, "登录失败"),
    // 登出
    PASSPORT_LOGOUT_FAILURE(0, "登出失败"), PASSPORT_UNBIND_FAILURE(0, "账号解绑失败"),
    PASSPORT_DELETE_ACCOUNT_STATUS_FAIL(0, "删除账户状态失败，登出失败"), PASSPORT_GET_STATUS_FAILURE(0, "获取账户状态失败"),

    // 以下为扩展登录异常
    PASSPORT_COMMUNICATION_ACCOUNT_NOT_FIND(1001, "云指挥找不到通信账号"), PASSPORT_CLOUD_ACCOUNT_NOT_FIND(1002, "用户名或密码错误"),
    PASSPORT_NOT_FIND_RESOURCE_BY_CLOUDACC(1003, "云指挥账号未绑定资源"),
    PASSPORT_NOT_FIND_RESOURCE_BY_COMMUNICATIONACC(1004, "未找到通信账号绑定的资源，账号："),
    PASSPORT_NOT_FIND_CLOUDACC_BY_RESOURCE(1005, "资源未绑定云指挥账号"),
    PASSPORT_NOT_FIND_COMMUNICATIONACC_BY_RESOURCE(1006, "资源未绑定通信账号"),
    PASSPORT_RESOURCE_BOUNDED_FOR_ATHER_ACCOUNT(1007, "资源被其他通信账号绑定了，资源id："),
    PASSPORT_ACCOUNT_BOUND_FOR_OTHER_RESOURCE(1008, "云指挥通信账号已被占用"),
    PASSPORT_NOT_FIND_ACCOUNTRES_BY_TOKEN(1009, "通过token获取账户资源信息失败"),
    PASSPORT_NOT_FIND_TOKEN_BY_RESOURCEID(1010, "通过资源id获取token失败，资源ID："),
    // 容器内容号码登录异常
    PASSPORT_NOT_FIND_CONTAINER_CONTENT_NUMBER(1011, "未找到容器内容信息，容器内容号码："),
    PASSPORT_NOT_FIND_CONTINERID_BY_CONTAINER_CONTENT_NUMBER(1012, "未找到容器内容号码对应的容器ID,号码："),
    PASSPORT_ACCOUNT_NOT_FIND(1013, "账号不存在"), PASSPORT_GPS_ACCOUNT_NOT_FIND(1014, "云指挥找不到GPS账号"),
    PASSPORT_GPS_ACCOUNT_BOUND_FOR_OTHER_RESOURCE(1015, "云指挥GPS账号已被占用"),
    PASSPORT_CLOUD_ACCOUNT_IS_EFFECTIVE(1016, "云指挥账号已过期"), PASSPORT_GPS_ACCOUNT_IS_EFFECTIVE(1017, "云指挥GPS账号已过期"),
    PASSPORT_COMMUNICATION_ACCOUNT_IS_EFFECTIVE(1018, "云指挥通信账号已过期"),

    // 以下为通用异常,
    COMMON_ERROR_300(2001, "ResultCode_COMMON_ERROR_300"), COMMON_ERROR_301(2002, "非法方法名，或该方法名不存在"),
    COMMON_ERROR_302(2003, "未知请求类型或不支持当前请求方法"), COMMON_ERROR_303(2004, "不支持当前媒体类型"), COMMON_ERROR_304(2005, "参数解析错误"),
    COMMON_ERROR_305(2006, "参数验证错误"), COMMON_ERROR_306(2007, "用户尚未登录"), COMMON_ERROR_307(2008, "调用链接失效，超过规定时效"),
    PASSPORT_SIGN_VERIFICATION_FAILED(2009, "Sign签名验证失败"), PASSPORT_MISS_PARAM(2010, "ResultCode_PASSPORT_MISS_PARAM"),
    PASSPORT_COMMUNICATION_ACCOUNT_NOT_EXIST(2011, "通信账号数据不存在"), PASSPORT_BIND_ERROR(2012, "账户资源绑定失败"),
    PASSPORT_REDIS_ERROR(2013, "存储登录信息失败"), PASSPORT_RESOURCE_OFFLINE(2014, "该资源不在线"),
    PASSPORT_ACCOUNT_STATUS_NOT_FIND(2015, "账号状态信息数据不存在"),

    PASSPORT_COMMUNICATION_OFFLINE(80004, "通信服务登录超时"),

    // 登录保活
    PASSPORT_LOST_HEARTBEAT(3001, "已失去心跳"),

    // 登录异常
    PASSPORT_REGISTEATION_RESOURCE(4011, "资源注册"), PASSPORT_COMMUNICATION_LOGIN_OUT(4012, "通信服务已退出登录"),
    PASSPORT_RESOURCE_NOT_AUTHORIZED(4013, "资源未授权"), PASSPORT_RESOURCE_NOT_FIND(4014, "资源不存在"),
    PASSPORT_RESOURCE_TEMPORARILY_UNAVAILABLE(4015, "资源暂时不可用"), PASSPORT_RESOURCE_CONFLICT(4016, "资源冲突"),
    PASSPORT_NO_AUTHENTICATION(4017, "未鉴权"), PASSPORT_LICENSE_LIMITED(4018, "License受限"),
    PASSPORT_PASSPORD_ERROR(4019, "用户名或密码错误"), PASSPORT_ACCOUNT_RESOURCE_UNBIND_FAIL(4020, "账号和资源解绑失败"),
    // 无效登录,
    PASSPORT_ERROR_LOGIN_TYPE(4021, "错误的登录类型"),

    // 账户密码格式校验
    PASSPORT_ACCOUNT_NAME_OR_PASSWORD_NOT_IS_NULL(0, "用户名或密码不能为空"),
    PASSPORT_ACCOUNT_NAME_HAS_ILLEGAL_CHARACTER(0, "账户名含有非法字符"), PASSPORT_ACCOUNT_NAME_LENGTH_ERROR(0, "用户名不能多于20字符"),
    PASSPORT_PASSWORD_HAS_ILLEGAL_CHARACTER(0, "密码中含有非法字符"), PASSPORT_PASSWORD_LENGTH_TOO_LONG(0, " 密码不能不多于20个字符"),
    PASSPORT_PASSWORD_LENGTH_TOO_SHORT(0, "密码不能少于6个字符");

    private Integer code;

    private String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}
