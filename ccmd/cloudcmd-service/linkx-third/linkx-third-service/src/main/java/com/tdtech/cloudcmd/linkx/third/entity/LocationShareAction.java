package com.tdtech.cloudcmd.linkx.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 位置共享信息表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_location_share_action")
public class LocationShareAction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 位置共享创建人ID（警信IM用户ID）
     */
    private Long userId;

    /**
     * 位置共享使用的UDC群组号码，用于组呼业务支持
     */
    private String udcGroup;

    /**
     * 位置共享状态。0：无效；1：有效
     */
    private int status;

    /**
     * 位置共享结束时间
     */
    private LocalDateTime closeTime;

    /**
     * 位置共享创建时间
     */
    private LocalDateTime gmtCreated;

}
