package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_tasks_favorite")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TasksFavorite implements Serializable {

    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 收藏动作的发起人身份证号
     */
    private String opUserId;

    /**
     * 收藏动作的发起人姓名
     */
    private String opUserName;

    /**
     * 收藏动作的发起人所属组织
     */
    private String opUserDepartment;

    /**
     * 操作时间
     */
    private Date operateTime;

}