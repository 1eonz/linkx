package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("tb_im_user_icp")
public class IcpImUser {

    @TableId
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
    private Integer refreshFlag;//1 没刷新 0 刷新了
}
