package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @author lsc
 * @date 2025/7/15
 **/
@Data
@TableName("tb_label")
public class Label {
    private Long id; // 主键ID
    private String name; // 标签名
    private Long parentId; // 父级标签ID
    private Integer level; // 标签层级（1, 2, 3）
    private String icon; // 图标
    private String color;  // color
    private Integer type; // 0 普通 1 1比14E
    private Integer isDeleted = 0;
    private Integer scope;//范围，0、全部，1、一键建群，2、职能建群
    private Date gmtCreated;//创建时间
    private Date gmtModified;//修改时间

    @TableField(exist = false)
    private Integer isCancel;//0:更新，1：取消

    @TableField(exist = false)
    private Integer isAssociatedCoop; //是否为最底层标签，0：否，1：是
}
