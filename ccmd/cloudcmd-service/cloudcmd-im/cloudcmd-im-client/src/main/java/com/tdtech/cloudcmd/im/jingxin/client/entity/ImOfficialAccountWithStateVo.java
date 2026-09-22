package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImOfficialAccountWithStateVo {

    /**
     * 公众号ID
     */
    private Long id;
    /**
     * 公众号名称
     */
    private String name;
    /**
     * 公众号类型 0-广播号 1-订阅号
     */
    private Integer category;
    /**
     * 公众号图标URL
     */
    private String iconUrl;
    /**
     * 备注
     */
    private String remark;
    /**
     * 欢迎语
     */
    private String welcome;
    /**
     * 状态 0-正常 1-禁用。预留，当前未使用
     */
    private Integer status;
    /**
     * 状态 false-正常 true-已删除
     */
    private Boolean isDel;
    /**
     * 所属部门ID
     */
    private Long departmentId;
    /**
     * 所属部门编号
     */
    private String departmentCode;
    /**
     * 所属部门名称
     */
    private String departmentName;
    /**
     * 创建时间。UTC时间戳。单位：毫秒
     */
    private Long createdTime;
    /**
     * 变更标识
     */
    private Long tag;
    /**
     * 文章总数
     */
    private Integer articleNum;
    /**
     * 订阅用户总数
     */
    private Integer userNum;
}
