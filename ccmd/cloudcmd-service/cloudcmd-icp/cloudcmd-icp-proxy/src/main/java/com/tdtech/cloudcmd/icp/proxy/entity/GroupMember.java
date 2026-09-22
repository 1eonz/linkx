package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("tb_group_member")
public class GroupMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * UDC上的群组号码
     */
    private Long group;

    /**
     * 群组成员的isdn
     */
    private String isdn;

    /**
     * 成员类型。1：用户， 2：群组
     */
    private int membertype;

    /**
     * 优先级（1~15）
     */
    private int userpriority;

}
