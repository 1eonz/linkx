package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author: S063874
 * @date: 2026-03-10 14:08
 */
@TableName("tb_system_layout_section")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLayoutSection {

    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 板块名称
     */
    private String name;

    /**
     * 板块类型。1：轮播图；2：常用应用；3：协同群组；4：三方网页；5：分割条；6：消息列表；
     */
    @TableField("`type`")
    private Integer type;

    /**
     * 板块点击的跳转URL
     */
    private String url;

    /**
     * 自定义参数
     */
    private String custom;

    /**
     * 应用排序，越小越靠前
     */
    private Integer sort;

    /**
     * 是否在前台展示。0-不展示，1-展示（default）
     */
    @TableField("`show`")
    private Integer show;

    /**
     * 板块是否被删除。0-未删除（default），1-已删除
     */
    private Integer deleted;

    /**
     * 最后修改时间
     */
    private Date gmtLastModified;

    /**
     * 创建时间
     */
    private Date gmtCreateTime;
}
