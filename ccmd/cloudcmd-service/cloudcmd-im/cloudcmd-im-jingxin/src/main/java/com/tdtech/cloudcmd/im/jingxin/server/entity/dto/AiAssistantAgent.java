package com.tdtech.cloudcmd.im.jingxin.server.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 虚拟用户信息表
 */
@Data
@TableName(value = "`linkx_auth`.`tr_ai_assistant_agent`")
public class AiAssistantAgent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @NotNull(message = "主键ID不能为null")
    private Long id;

    /**
     * 虚拟用户id
     */
    @TableField(value = "`virtual_user_id`")
    @NotNull(message = "虚拟用户id不能为null")
    private Long virtualUserId;

    /**
     * AI智能体ID
     */
    @TableField(value = "`agent_id`")
    @NotNull(message = "AI智能体ID不能为null")
    private Long agentId;

    /**
     * 创建人ID
     */
    @TableField(value = "`created_user_id`")
    @NotNull(message = "创建人ID不能为null")
    private Long createdUserId;

    @TableField(value = "`gmt_created`")
    private Date gmtCreated;
}