package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_location_share_member")
public class LocationShareMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     *  tb_location_share_action表的id
     */
    private Long locationShareActionId;

    /**
     * 警信用户ID
     */
    private Long userId;

    /**
     * 警信用户绑定的ISDN
     */
    private String isdn;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 允许位置共享的开始时间
     */
    private LocalDateTime gisShareStartTime;

    /**
     * 允许位置共享的结束时间
     */
    private LocalDateTime gisShareEndTime;

    /**
     * 用户退出位置共享方式。0：手工退出；1：共享时间结束；2：用户状态异常
     */
    private int exitType;

    /**
     * 用户退出位置共享的时间
     */
    private LocalDateTime exitTime;

    /**
     * 用户退出描述
     */
    private String exitDesc;
}
