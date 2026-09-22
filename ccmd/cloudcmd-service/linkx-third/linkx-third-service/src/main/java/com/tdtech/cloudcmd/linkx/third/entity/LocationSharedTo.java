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
 * 位置共享被分发的目标会话表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_location_shared_to")
public class LocationSharedTo implements Serializable {

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
     * 位置共享分发人ID（警信IM用户ID）
     */
    private Long sharedBy;

    /**
     * 位置共享分发的目标对象类型。1：警信群组；2：警信用户
     */
    private int sharedTargetType;

    /**
     * 位置共享分发的目标对象会话ID
     */
    private Long sharedTargetSessionId;

    /**
     * 位置共享分发的时间
     */
    private LocalDateTime sharedTime;

}
