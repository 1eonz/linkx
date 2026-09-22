package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserResp {


    /**
     * 别名
     */
    private String alias;

    /**
     * 应用类型
     * 0：未配置
     * 1：民警
     * 4：基地台
     * 102：处警警车
     * 103：应急指挥车
     * 104：消防车
     * 105：押运车
     * 106：公务警车
     * 107：警用摩托车
     */
    private String apptype;

    /**
     * 用户类型
     * 0：调度台用户
     * 1：固定摄像头
     * 2：PSTN网关用户
     * 3：TETRA网关用户
     * 4：PLMN网关用户
     * 5：外部PTT用户
     * 6：网关融合用户
     * 7：公网APP用户
     * 8：终端登陆用户
     * 9：终端用户
     * 10：网关代理用户
     * 11：视频记录仪
     * 12：有线视频终端用户
     * 255：未分类用户
     */
    private String category;

    /**
     * 部门ID
     */
    private String departmentid;

    /**
     * 用户号
     */
    private String isdn;

    /**
     * 名称
     */
    private String name;

    /**
     * 优先级（1~15）
     */
    private String priority;

    /**
     * 子用户类型
     * 0：终端
     * 1：鼎桥记录仪
     * 3：布控球
     */
    private String subusercategory;

    /**
     * 终端类型
     * 0：本地用户
     * 1：外部用户
     */
    private String uetype;

    /**
     * VPN ID
     */
    @TableField(exist = false)
    private String vpnid;

    /**
     * VPN入站
     */
    @TableField(exist = false)
    private String vpnin;

    /**
     * VPN出站
     */
    @TableField(exist = false)
    private String vpnout;

}
