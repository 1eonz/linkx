package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_group")
public class Group {

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 群组名称
     */
    private String name;

    /**
     * 群组用途。0：常规通信；1：位置共享；2：会议
     */
    private int purpose;

    /**
     * 群组类型。1：普通组；9：动态组
     */
    private int category;

    /**
     * 群组号
     */
    private String group;

    /**
     * 群组状态。0：disable, 1：enable
     */
    private boolean grpstate;

    /**
     * 优先级（1~15）
     */
    private int priority;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreated;

}
