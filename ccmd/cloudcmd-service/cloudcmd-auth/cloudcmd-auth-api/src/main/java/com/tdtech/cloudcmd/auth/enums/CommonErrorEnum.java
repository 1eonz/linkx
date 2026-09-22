package com.tdtech.cloudcmd.auth.enums;

/**
 * @author zWX523748
 * @date 2020/6/1 16:46
 */
public enum CommonErrorEnum {

    COMMON_ERROR_100(100, "CommonErrorEnum_COMMON_ERROR_100"), // "内部错误，请稍后重试"
    COMMON_ERROR_101(101, "CommonErrorEnum_COMMON_ERROR_101"), // "非法方法名，或该方法名不存在"
    COMMON_ERROR_102(102, "CommonErrorEnum_COMMON_ERROR_102"), // "未知请求类型或不支持当前请求方法"
    COMMON_ERROR_103(103, "CommonErrorEnum_COMMON_ERROR_103"), // "不支持当前媒体类型"
    COMMON_ERROR_104(104, "CommonErrorEnum_COMMON_ERROR_104"), // "缺少参数，参数："
    COMMON_ERROR_105(105, "CommonErrorEnum_COMMON_ERROR_105"), // "参数解析错误"
    COMMON_ERROR_106(106, "CommonErrorEnum_COMMON_ERROR_106"), // "参数验证错误"
    COMMON_ERROR_107(107, "CommonErrorEnum_COMMON_ERROR_107"), // "登录Token验证失败"
    COMMON_ERROR_108(108, "CommonErrorEnum_COMMON_ERROR_108"), // "Sign签名验证失败"
    COMMON_ERROR_109(109, "CommonErrorEnum_COMMON_ERROR_109"), // "调用链接失效，超过规定时效"
    COMMON_ERROR_110(110, "CommonErrorEnum_COMMON_ERROR_110"), // "用户不存在"
    COMMON_ERROR_111(111, "CommonErrorEnum_COMMON_ERROR_111"), // "账户已禁用"
    COMMON_ERROR_112(112, "CommonErrorEnum_COMMON_ERROR_112"), // "账户已冻结"
    COMMON_ERROR_113(113, "CommonErrorEnum_COMMON_ERROR_113"), // "用户或者密码错误，请核对后重新输入"
    COMMON_ERROR_114(114, "CommonErrorEnum_COMMON_ERROR_114"), // "首次登陆，请修改密码"
    COMMON_ERROR_115(115, "CommonErrorEnum_COMMON_ERROR_115"), // "密码过期，请修改密码"
    COMMON_ERROR_116(116, "CommonErrorEnum_COMMON_ERROR_116"), // "刷新token不存在或过期"
    COMMON_ERROR_117(117, "CommonErrorEnum_COMMON_ERROR_117"), // token超时
    COMMON_ERROR_118(118, "CommonErrorEnum_COMMON_ERROR_118"), // token不存在
    COMMON_ERROR_119(119, "CommonErrorEnum_COMMON_ERROR_119"), // 新密码复杂度不够
    COMMON_ERROR_120(120, "CommonErrorEnum_COMMON_ERROR_120"), // 两次输入的新密码不一致
    COMMON_ERROR_121(121, "CommonErrorEnum_COMMON_ERROR_121"), // 密码即将过期
    COMMON_ERROR_122(122, "CommonErrorEnum_COMMON_ERROR_122"), // 用户已过期，请联系管理员
    COMMON_ERROR_123(123, "CommonErrorEnum_COMMON_ERROR_123"), // 新旧密码相同
    COMMON_ERROR_124(124, "CommonErrorEnum_COMMON_ERROR_124"), // 未绑定装备
    COMMON_ERROR_125(125, "CommonErrorEnum_COMMON_ERROR_125"), // 终端类型不匹配。
    COMMON_ERROR_130(130, "CommonErrorEnum_COMMON_ERROR_130"), // 警号未关联用户
    COMMON_ERROR_131(131, "CommonErrorEnum_COMMON_ERROR_131"), // 用户未关联警号
    COMMON_ERROR_132(132, "CommonErrorEnum_COMMON_ERROR_132"), // 手机号未关联用户
    COMMON_ERROR_133(133, "CommonErrorEnum_COMMON_ERROR_133"), // 该用户暂无角色信息,请联系管理员
    COMMON_ERROR_134(134, "CommonErrorEnum_COMMON_ERROR_134"), // 旧密码错误
    COMMON_ERROR_135(135, "CommonErrorEnum_COMMON_ERROR_135"), // 新密码不符合规范， 新密码必须包含大小写字母、数字、特殊字符，且长度不少于8位
    COMMON_ERROR_136(136, "CommonErrorEnum_COMMON_ERROR_136"), // 修改密码操作频繁
    COMMON_ERROR_137(137, "CommonErrorEnum_COMMON_ERROR_137"),// 密码已被重置，请重新修改密码
    COMMON_ERROR_140(140, "CommonErrorEnum_COMMON_ERROR_140"), // 账户密码错误，账户已超过5次 。
    COMMON_ERROR_141(141, "CommonErrorEnum_COMMON_ERROR_141"), // 密码包含超过两个连续相同字符。
    COMMON_ERROR_142(142, "CommonErrorEnum_COMMON_ERROR_142"),  //用户名、密码长度超长  。
    COMMON_ERROR_143(143, "CommonErrorEnum_COMMON_ERROR_143"),  //用户名、密码长度超长  。
    COMMON_ERROR_144(144, "CommonErrorEnum_COMMON_ERROR_144"),  //用户名、密码长度超长  。
    COMMON_ERROR_145(145, "CommonErrorEnum_COMMON_ERROR_145"),  //不允许修改除自身账号以外的口令
    COMMON_ERROR_146(146, "CommonErrorEnum_COMMON_ERROR_146"), // LICENSE基础功能不可用，请重新导入LICENSE。ICC、CAPP、ADMIN不允许登录
    COMMON_ERROR_147(147, "CommonErrorEnum_COMMON_ERROR_147"), // LICENSE基础功能不可用，请重新导入LICENSE。ICC、CAPP、ADMIN不允许登录
    COMMON_ERROR_148(148, "CommonErrorEnum_COMMON_ERROR_148"), // 密码不能和账号一样
    COMMON_ERROR_149(149, "CommonErrorEnum_COMMON_ERROR_149"), // 密码不能为空
    COMMON_ERROR_150(150, "CommonErrorEnum_COMMON_ERROR_150"); // 新密码必须包含大小写字母、数字、特殊字符中至少两种字符的组合，且长度不少于8位

    private int code;
    private String msg;

    private CommonErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
