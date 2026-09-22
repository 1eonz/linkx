package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Data
@TableName("tb_create_group")
public class CreateGroup {
    private Long id;

    private String ownerId;
    private String ownerName;

    /**
     * 名称是ID，实际上是组织编码。。狗屎玩意
     */
    private String departmentId;
    private String departmentName;

    private Date createTime;

    private Date updateTime;
    private String userIds;
    private String userNames;

    private Long groupId;

    private String groupName;

    /**
     * 位置：经纬度，格式：经度,纬度
     */
    private String location;

    /**
     * 群头像
     */
    private String avatar;
    /**
     * 群头像图片
     */
    private String avatarImg;

    /**
     * 来源 0 其他 1 一键建群 2 警单建群 3 自定义建群 4 职能建群 5 一键调度
     */
    private Integer source;

    /**
     * 是否存在：1 存在，0 已解散
     */
    private Integer existed = 1;
}
