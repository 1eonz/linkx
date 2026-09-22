package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("tb_icon")
@Data
public class Icon {
    /**
     * 主键id
     */
    @TableId
    private Long id;
    /**
     * 图标地址信息
     */
    @TableField("icon_info")
    private String iconInfo;
    /**
     * 图标类型：1.标绘图标 2.车辆幻化
     */
    @TableField("type")
    private Integer type;

}
